package com.ecommerce.inventory.application.port.out;

import com.ecommerce.inventory.domain.model.InventoryItem;

import java.util.Optional;
import java.util.UUID;

public interface InventoryRepositoryPort {

    Optional<InventoryItem> findByProductId(UUID productId);

    InventoryItem save(InventoryItem item);
}
