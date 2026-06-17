package com.ecommerce.product.infrastructure.persistence;

import com.ecommerce.product.domain.model.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories")
public class CategoryEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "parent_id")
    private UUID parentId;

    @Column(nullable = false)
    private Instant createdAt;

    protected CategoryEntity() {
    }

    public static CategoryEntity fromDomain(Category c) {
        CategoryEntity e = new CategoryEntity();
        e.id = c.id();
        e.name = c.name();
        e.parentId = c.parentId();
        e.createdAt = c.createdAt();
        return e;
    }

    public Category toDomain() {
        return new Category(id, name, parentId, createdAt);
    }
}
