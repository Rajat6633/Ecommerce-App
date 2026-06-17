package com.ecommerce.common.events.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Emitted by order-service to start the order saga. */
public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        String currency,
        BigDecimal totalAmount,
        List<OrderLineItem> items
) {
    public record OrderLineItem(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}
