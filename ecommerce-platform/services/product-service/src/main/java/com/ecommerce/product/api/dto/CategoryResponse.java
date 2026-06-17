package com.ecommerce.product.api.dto;

import com.ecommerce.product.domain.model.Category;

import java.time.Instant;
import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name,
        UUID parentId,
        Instant createdAt
) {
    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.id(), c.name(), c.parentId(), c.createdAt());
    }
}
