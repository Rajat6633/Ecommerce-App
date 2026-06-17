package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservationLine;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservationOutcome;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase.Status;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.application.port.out.ProcessedEventPort;
import com.ecommerce.inventory.application.port.out.ReservationRepositoryPort;
import com.ecommerce.inventory.domain.model.InventoryItem;
import com.ecommerce.inventory.domain.model.ReservationStatus;
import com.ecommerce.inventory.domain.model.StockReservation;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockReservationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock ReservationRepositoryPort reservationRepository;
    @Mock ProcessedEventPort processedEvents;

    private StockReservationService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();
    private final UUID p1 = UUID.randomUUID();
    private final UUID p2 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new StockReservationService(inventoryRepository, reservationRepository,
                processedEvents, Clock.fixed(NOW, ZoneOffset.UTC), new SimpleMeterRegistry());
    }

    private InventoryItem item(UUID productId, int onHand, int reserved) {
        return new InventoryItem(UUID.randomUUID(), productId, onHand, reserved, 0, 0L);
    }

    @Test
    void reserve_allInStock_reservesAndReturnsReserved() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.of(item(p1, 10, 0)));
        when(inventoryRepository.findByProductId(p2)).thenReturn(Optional.of(item(p2, 5, 0)));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        ReservationOutcome outcome = service.reserveForOrder(eventId, orderId,
                List.of(new ReservationLine(p1, 2), new ReservationLine(p2, 1)));

        assertThat(outcome.status()).isEqualTo(Status.RESERVED);
        assertThat(outcome.reservedItems()).hasSize(2);
        verify(inventoryRepository, org.mockito.Mockito.times(2)).save(any());
        verify(reservationRepository).saveAll(anyList());
    }

    @Test
    void reserve_insufficientStock_returnsFailed_withoutMutation() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.of(item(p1, 1, 0)));

        ReservationOutcome outcome = service.reserveForOrder(eventId, orderId,
                List.of(new ReservationLine(p1, 5)));

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        verify(inventoryRepository, never()).save(any());
        verify(reservationRepository, never()).saveAll(anyList());
    }

    @Test
    void reserve_unknownProduct_returnsFailed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(false);
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.empty());

        ReservationOutcome outcome = service.reserveForOrder(eventId, orderId,
                List.of(new ReservationLine(p1, 1)));

        assertThat(outcome.status()).isEqualTo(Status.FAILED);
        verify(reservationRepository, never()).saveAll(anyList());
    }

    @Test
    void reserve_duplicateEvent_returnsAlreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        ReservationOutcome outcome = service.reserveForOrder(eventId, orderId,
                List.of(new ReservationLine(p1, 1)));

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void reserve_orderAlreadyReserved_returnsAlreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.existsByOrderId(orderId)).thenReturn(true);

        ReservationOutcome outcome = service.reserveForOrder(eventId, orderId,
                List.of(new ReservationLine(p1, 1)));

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void release_activeReservations_releasesStockAndReturnsTrue() {
        StockReservation res = new StockReservation(UUID.randomUUID(), orderId, p1, 2,
                ReservationStatus.RESERVED, NOW);
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of(res));
        when(inventoryRepository.findByProductId(p1)).thenReturn(Optional.of(item(p1, 10, 2)));
        when(inventoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        boolean released = service.releaseForOrder(eventId, orderId);

        assertThat(released).isTrue();
        verify(inventoryRepository).save(any());
        verify(reservationRepository).save(any());
    }

    @Test
    void release_nothingReserved_returnsFalse() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(reservationRepository.findByOrderId(orderId)).thenReturn(List.of());

        assertThat(service.releaseForOrder(eventId, orderId)).isFalse();
    }

    @Test
    void release_duplicateEvent_returnsFalse() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        assertThat(service.releaseForOrder(eventId, orderId)).isFalse();
        verify(reservationRepository, never()).findByOrderId(any());
    }
}
