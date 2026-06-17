package com.ecommerce.product.api.dto;

import com.ecommerce.product.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        UUID categoryId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductResponse from(Product p) {
        return new ProductResponse(p.id(), p.sku(), p.name(), p.description(), p.price(),
                p.currency(), p.categoryId(), p.active(), p.createdAt(), p.updatedAt());
    }
}
