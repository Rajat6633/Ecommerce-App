package com.ecommerce.product.domain.model;

import java.time.Instant;
import java.util.UUID;

/** Category aggregate; {@code parentId} is null for a root category. */
public record Category(
        UUID id,
        String name,
        UUID parentId,
        Instant createdAt
) {
    public Category {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("category name must not be blank");
        }
    }
}
