package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by order-service once an order is fully confirmed (paid + reserved). */
public record OrderConfirmedEvent(
        UUID orderId,
        UUID userId
) {
}
