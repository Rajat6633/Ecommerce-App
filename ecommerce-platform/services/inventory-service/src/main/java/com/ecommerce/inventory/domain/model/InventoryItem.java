package com.ecommerce.inventory.domain.model;

import com.ecommerce.inventory.domain.exception.InsufficientStockException;

import java.util.UUID;

/**
 * Inventory aggregate for a product. {@code available = onHand - reserved}.
 * Mutating operations return a new instance (immutable domain); the persistence
 * layer applies optimistic locking via {@code version}.
 */
public record InventoryItem(
        UUID id,
        UUID productId,
        int onHand,
        int reserved,
        int reorderLevel,
        Long version
) {
    public InventoryItem {
        if (onHand < 0) throw new IllegalArgumentException("onHand must be >= 0");
        if (reserved < 0) throw new IllegalArgumentException("reserved must be >= 0");
    }

    public static InventoryItem create(UUID id, UUID productId, int onHand, int reorderLevel) {
        return new InventoryItem(id, productId, onHand, 0, reorderLevel, null);
    }

    public int available() {
        return onHand - reserved;
    }

    public boolean canReserve(int quantity) {
        return quantity > 0 && available() >= quantity;
    }

    /** Reserve stock, raising {@link InsufficientStockException} if not possible. */
    public InventoryItem reserve(int quantity) {
        if (!canReserve(quantity)) {
            throw new InsufficientStockException(productId, quantity, available());
        }
        return new InventoryItem(id, productId, onHand, reserved + quantity, reorderLevel, version);
    }

    /** Release a previously held reservation (never goes below zero). */
    public InventoryItem release(int quantity) {
        int newReserved = Math.max(0, reserved - quantity);
        return new InventoryItem(id, productId, onHand, newReserved, reorderLevel, version);
    }

    /** Receive new stock (restock). */
    public InventoryItem receive(int quantity) {
        return new InventoryItem(id, productId, onHand + quantity, reserved, reorderLevel, version);
    }

    /** Set absolute on-hand quantity (admin adjustment). */
    public InventoryItem withOnHand(int newOnHand) {
        return new InventoryItem(id, productId, newOnHand, reserved, reorderLevel, version);
    }
}
