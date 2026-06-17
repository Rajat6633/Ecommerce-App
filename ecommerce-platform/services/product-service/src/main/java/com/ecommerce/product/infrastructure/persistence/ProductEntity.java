package com.ecommerce.product.infrastructure.persistence;

import com.ecommerce.product.domain.model.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products")
public class ProductEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected ProductEntity() {
    }

    public static ProductEntity fromDomain(Product p) {
        ProductEntity e = new ProductEntity();
        e.id = p.id();
        e.sku = p.sku();
        e.name = p.name();
        e.description = p.description();
        e.price = p.price();
        e.currency = p.currency();
        e.categoryId = p.categoryId();
        e.active = p.active();
        e.version = p.version();
        e.createdAt = p.createdAt();
        e.updatedAt = p.updatedAt();
        return e;
    }

    public Product toDomain() {
        return new Product(id, sku, name, description, price, currency, categoryId,
                active, version, createdAt, updatedAt);
    }
}
