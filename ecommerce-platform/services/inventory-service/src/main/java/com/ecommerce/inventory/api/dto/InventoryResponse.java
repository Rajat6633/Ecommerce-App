package com.ecommerce.inventory.api.dto;

import com.ecommerce.inventory.domain.model.InventoryItem;

import java.util.UUID;

public record InventoryResponse(
        UUID productId,
        int onHand,
        int reserved,
        int available,
        int reorderLevel
) {
    public static InventoryResponse from(InventoryItem i) {
        return new InventoryResponse(i.productId(), i.onHand(), i.reserved(), i.available(), i.reorderLevel());
    }
}
