package com.ecommerce.inventory.application.service;

import com.ecommerce.inventory.application.port.in.InventoryAdminUseCase;
import com.ecommerce.inventory.application.port.out.InventoryRepositoryPort;
import com.ecommerce.inventory.domain.exception.InventoryItemNotFoundException;
import com.ecommerce.inventory.domain.model.InventoryItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryService implements InventoryAdminUseCase {

    private final InventoryRepositoryPort inventoryRepository;

    public InventoryService(InventoryRepositoryPort inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryItem getByProduct(UUID productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId.toString()));
    }

    @Override
    @Transactional
    public InventoryItem upsertStock(UUID productId, int onHand, int reorderLevel) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .map(existing -> new InventoryItem(existing.id(), existing.productId(), onHand,
                        existing.reserved(), reorderLevel, existing.version()))
                .orElseGet(() -> InventoryItem.create(UUID.randomUUID(), productId, onHand, reorderLevel));
        return inventoryRepository.save(item);
    }

    @Override
    @Transactional
    public InventoryItem receiveStock(UUID productId, int quantity) {
        InventoryItem item = inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryItemNotFoundException(productId.toString()));
        return inventoryRepository.save(item.receive(quantity));
    }
}
