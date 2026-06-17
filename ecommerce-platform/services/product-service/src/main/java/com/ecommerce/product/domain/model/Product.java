package com.ecommerce.product.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Product aggregate. {@code version} backs optimistic locking at the
 * persistence layer. Price is a {@link BigDecimal} with an ISO currency code.
 */
public record Product(
        UUID id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        String currency,
        UUID categoryId,
        boolean active,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public Product {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException("sku must not be blank");
        }
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must be >= 0");
        }
        if (currency == null || currency.length() != 3) {
            throw new IllegalArgumentException("currency must be a 3-letter ISO code");
        }
    }

    /** Factory for a brand-new product (no id/version yet assigned by JPA). */
    public static Product create(UUID id, String sku, String name, String description,
                                 BigDecimal price, String currency, UUID categoryId, Instant now) {
        return new Product(id, sku, name, description, price, currency, categoryId, true, null, now, now);
    }

    /** Returns a copy with mutable fields updated (id/sku/version preserved). */
    public Product withUpdates(String name, String description, BigDecimal price,
                               String currency, UUID categoryId, boolean active, Instant now) {
        return new Product(id, sku, name, description, price, currency, categoryId, active,
                version, createdAt, now);
    }
}
