package com.ecommerce.common.events.payload;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Emitted by inventory-service when all stock for an order is reserved.
 * Carries the order amount/currency so payment-service can charge without an
 * extra lookup (the values originate from order.created).
 */
public record InventoryReservedEvent(
        UUID orderId,
        BigDecimal amount,
        String currency,
        List<ReservedLine> reservations
) {
    public record ReservedLine(UUID productId, int quantity) {
    }
}
