package com.ecommerce.order.application.service;

import com.ecommerce.order.application.port.in.OrderSagaUseCase;
import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.application.port.out.ProcessedEventPort;
import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

/**
 * Saga reactions. Each handler is @Transactional so the idempotency record
 * (firstSeen) commits atomically with the status change; the consumer publishes
 * any follow-on event after this method returns (post-commit).
 */
@Service
public class OrderSagaService implements OrderSagaUseCase {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaService.class);

    private final OrderRepositoryPort orderRepository;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;
    private final Counter ordersCompleted;
    private final Counter ordersFailed;

    public OrderSagaService(OrderRepositoryPort orderRepository,
                            ProcessedEventPort processedEvents,
                            Clock clock,
                            MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.processedEvents = processedEvents;
        this.clock = clock;
        this.ordersCompleted = Counter.builder("orders_completed_total")
                .description("Total orders confirmed").register(meterRegistry);
        this.ordersFailed = Counter.builder("orders_failed_total")
                .description("Total orders rejected or payment-failed").register(meterRegistry);
    }

    @Override
    @Transactional
    public void onInventoryReserved(UUID eventId, UUID orderId) {
        if (!processedEvents.firstSeen(eventId)) return;
        transition(orderId, OrderStatus.INVENTORY_RESERVED, "inventory reserved");
    }

    @Override
    @Transactional
    public Optional<Order> onPaymentCompleted(UUID eventId, UUID orderId) {
        if (!processedEvents.firstSeen(eventId)) return Optional.empty();
        Optional<Order> found = orderRepository.findById(orderId);
        if (found.isEmpty()) {
            log.warn("payment.completed for unknown order {}", orderId);
            return Optional.empty();
        }
        Order order = found.get();
        if (!order.canTransitionTo(OrderStatus.PAID)) {
            log.warn("Ignoring payment.completed for order {} in state {}", orderId, order.status());
            return Optional.empty();
        }
        Order paid = orderRepository.updateStatus(
                order.transitionTo(OrderStatus.PAID, clock.instant()), "payment completed");
        Order confirmed = orderRepository.updateStatus(
                paid.transitionTo(OrderStatus.CONFIRMED, clock.instant()), "order confirmed");
        ordersCompleted.increment();
        log.info("Order {} confirmed", orderId);
        return Optional.of(confirmed);
    }

    @Override
    @Transactional
    public void onPaymentFailed(UUID eventId, UUID orderId, String reason) {
        if (!processedEvents.firstSeen(eventId)) return;
        if (transition(orderId, OrderStatus.PAYMENT_FAILED, "payment failed: " + reason)) {
            ordersFailed.increment();
        }
    }

    @Override
    @Transactional
    public void onInventoryReservationFailed(UUID eventId, UUID orderId, String reason) {
        if (!processedEvents.firstSeen(eventId)) return;
        if (transition(orderId, OrderStatus.REJECTED, "rejected: " + reason)) {
            ordersFailed.increment();
        }
    }

    /** @return true if the transition was applied. */
    private boolean transition(UUID orderId, OrderStatus target, String note) {
        Optional<Order> found = orderRepository.findById(orderId);
        if (found.isEmpty()) {
            log.warn("Saga event for unknown order {}", orderId);
            return false;
        }
        Order order = found.get();
        if (!order.canTransitionTo(target)) {
            log.warn("Ignoring transition {} -> {} for order {}", order.status(), target, orderId);
            return false;
        }
        orderRepository.updateStatus(order.transitionTo(target, clock.instant()), note);
        return true;
    }
}
