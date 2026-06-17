package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.Order;

import java.util.Optional;
import java.util.UUID;

/**
 * Saga reactions to downstream events. Each method is idempotent (dedupe by
 * eventId) and transactional; the caller publishes any resulting event after
 * the transaction commits.
 */
public interface OrderSagaUseCase {

    void onInventoryReserved(UUID eventId, UUID orderId);

    /** @return the confirmed order if it just transitioned to CONFIRMED (publish order.confirmed). */
    Optional<Order> onPaymentCompleted(UUID eventId, UUID orderId);

    void onPaymentFailed(UUID eventId, UUID orderId, String reason);

    void onInventoryReservationFailed(UUID eventId, UUID orderId, String reason);
}
