package com.ecommerce.inventory.domain.model;

import java.time.Instant;
import java.util.UUID;

/** A single reserved line for an order (one row per order+product). */
public record StockReservation(
        UUID id,
        UUID orderId,
        UUID productId,
        int quantity,
        ReservationStatus status,
        Instant createdAt
) {
    public static StockReservation reserved(UUID id, UUID orderId, UUID productId, int quantity, Instant now) {
        return new StockReservation(id, orderId, productId, quantity, ReservationStatus.RESERVED, now);
    }

    public StockReservation released() {
        return new StockReservation(id, orderId, productId, quantity, ReservationStatus.RELEASED, createdAt);
    }
}
