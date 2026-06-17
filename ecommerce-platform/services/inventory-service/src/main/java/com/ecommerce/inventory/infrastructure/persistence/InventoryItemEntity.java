package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.domain.model.InventoryItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "inventory_items")
public class InventoryItemEntity {

    @Id
    private UUID id;

    @Column(name = "product_id", nullable = false, unique = true)
    private UUID productId;

    @Column(nullable = false)
    private int onHand;

    @Column(nullable = false)
    private int reserved;

    @Column(nullable = false)
    private int reorderLevel;

    @Version
    private Long version;

    protected InventoryItemEntity() {
    }

    public static InventoryItemEntity fromDomain(InventoryItem i) {
        InventoryItemEntity e = new InventoryItemEntity();
        e.id = i.id();
        e.productId = i.productId();
        e.onHand = i.onHand();
        e.reserved = i.reserved();
        e.reorderLevel = i.reorderLevel();
        e.version = i.version();
        return e;
    }

    public InventoryItem toDomain() {
        return new InventoryItem(id, productId, onHand, reserved, reorderLevel, version);
    }
}
