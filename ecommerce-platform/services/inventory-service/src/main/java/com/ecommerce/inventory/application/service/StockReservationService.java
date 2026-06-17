package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.StockReservationUseCase;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.application.port.out.ProcessedEventPort;
import com.ecommerce.inventory.application.port.out.ReservationRepositoryPort;
import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import com.ecommerce.inventory.domain.model.StockReservation;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Reservation/release saga logic. Validates ALL lines before mutating any, so a
 * failed reservation leaves no partial state. Idempotent per order. Publishing
 * is deferred to the caller (post-commit) via the returned outcome.
 */
@Service
public class StockReservationService implements StockReservationUseCase {

    private static final Logger log = LoggerFactory.getLogger(StockReservationService.class);

    private final InventoryRepositoryPort inventoryRepository;
    private final ReservationRepositoryPort reservationRepository;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;
    private final Counter reservedTotal;
    private final Counter reservationFailedTotal;

    public StockReservationService(InventoryRepositoryPort inventoryRepository,
                                   ReservationRepositoryPort reservationRepository,
                                   ProcessedEventPort processedEvents,
                                   Clock clock,
                                   MeterRegistry meterRegistry) {
        this.inventoryRepository = inventoryRepository;
        this.reservationRepository = reservationRepository;
        this.processedEvents = processedEvents;
        this.clock = clock;
        this.reservedTotal = Counter.builder("inventory_reserved_total")
                .description("Total stock line-items successfully reserved").register(meterRegistry);
        this.reservationFailedTotal = Counter.builder("inventory_reservation_failed_total")
                .description("Total order reservations that failed").register(meterRegistry);
    }

    @Override
    @Transactional
    public ReservationOutcome reserveForOrder(UUID eventId, UUID orderId, List<ReservationLine> lines) {
        // Dedupe commits in the SAME transaction as the reservation: if anything
        // below fails and rolls back, the event id is un-recorded and Kafka redelivers.
        if (!processedEvents.firstSeen(eventId) || reservationRepository.existsByOrderId(orderId)) {
            log.info("Order {} (event {}) already processed — skipping (idempotent)", orderId, eventId);
            return ReservationOutcome.alreadyProcessed();
        }

        // --- validate every line first (no mutation yet) ---
        List<InventoryItem> reservedItems = new ArrayList<>();
        for (ReservationLine line : lines) {
            Optional<InventoryItem> found = inventoryRepository.findByProductId(line.productId());
            if (found.isEmpty()) {
                return fail("No inventory for product " + line.productId());
            }
            InventoryItem item = found.get();
            if (!item.canReserve(line.quantity())) {
                return fail("Insufficient stock for product " + line.productId());
            }
            reservedItems.add(item.reserve(line.quantity()));
        }

        // --- all good: persist reservations + updated stock ---
        List<StockReservation> rows = new ArrayList<>();
        List<ReservedItem> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            ReservationLine line = lines.get(i);
            inventoryRepository.save(reservedItems.get(i));
            rows.add(StockReservation.reserved(UUID.randomUUID(), orderId, line.productId(),
                    line.quantity(), clock.instant()));
            result.add(new ReservedItem(line.productId(), line.quantity()));
        }
        reservationRepository.saveAll(rows);
        reservedTotal.increment(result.size());
        log.info("Reserved {} line(s) for order {}", result.size(), orderId);
        return ReservationOutcome.reserved(result);
    }

    @Override
    @Transactional
    public boolean releaseForOrder(UUID eventId, UUID orderId) {
        if (!processedEvents.firstSeen(eventId)) {
            log.info("Release event {} for order {} already processed — skipping", eventId, orderId);
            return false;
        }
        List<StockReservation> active = reservationRepository.findByOrderId(orderId).stream()
                .filter(r -> r.status() == ReservationStatus.RESERVED)
                .toList();
        if (active.isEmpty()) {
            return false; // nothing to release (idempotent)
        }
        for (StockReservation reservation : active) {
            inventoryRepository.findByProductId(reservation.productId())
                    .ifPresent(item -> inventoryRepository.save(item.release(reservation.quantity())));
            reservationRepository.save(reservation.released());
        }
        log.info("Released {} reservation(s) for order {}", active.size(), orderId);
        return true;
    }

    private ReservationOutcome fail(String reason) {
        reservationFailedTotal.increment();
        log.warn("Reservation failed: {}", reason);
        return ReservationOutcome.failed(reason);
    }
}
