package com.ecommerce.inventory.application.port.in;

import com.ecommerce.inventory.domain.model.InventoryItem;

import java.util.UUID;

/** Admin/query operations on inventory. */
public interface InventoryAdminUseCase {

    InventoryItem getByProduct(UUID productId);

    /** Create the item if absent, else set absolute on-hand + reorder level. */
    InventoryItem upsertStock(UUID productId, int onHand, int reorderLevel);

    /** Add stock (restock). */
    InventoryItem receiveStock(UUID productId, int quantity);
}
