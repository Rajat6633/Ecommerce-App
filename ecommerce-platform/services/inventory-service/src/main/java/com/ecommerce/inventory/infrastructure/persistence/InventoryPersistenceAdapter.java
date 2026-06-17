package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.model.InventoryItem;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class InventoryPersistenceAdapter implements InventoryRepositoryPort {

    private final InventoryItemJpaRepository repository;

    public InventoryPersistenceAdapter(InventoryItemJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<InventoryItem> findByProductId(UUID productId) {
        return repository.findByProductId(productId).map(InventoryItemEntity::toDomain);
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        return repository.save(InventoryItemEntity.fromDomain(item)).toDomain();
    }
}
