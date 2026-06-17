package com.ecommerce.inventory.domain.exception;

import java.util.UUID;

/** Raised when available stock cannot satisfy a reservation. */
public class InsufficientStockException extends InventoryException {
    public InsufficientStockException(UUID productId, int requested, int available) {
        super("Insufficient stock for product %s: requested=%d, available=%d"
                .formatted(productId, requested, available));
    }
}
