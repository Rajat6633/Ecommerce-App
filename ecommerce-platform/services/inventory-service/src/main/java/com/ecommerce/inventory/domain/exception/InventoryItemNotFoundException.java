package com.ecommerce.inventory.domain.exception;

/** -> HTTP 404. */
public class InventoryItemNotFoundException extends InventoryException {
    public InventoryItemNotFoundException(String productId) {
        super("No inventory for product: " + productId);
    }
}
