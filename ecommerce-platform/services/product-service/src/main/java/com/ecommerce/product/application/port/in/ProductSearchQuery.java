package com.ecommerce.product.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Search criteria + pagination/sorting. All filters are optional (null = ignore).
 * {@code sortBy} is validated against an allow-list in the persistence adapter.
 */
public record ProductSearchQuery(
        String name,
        UUID categoryId,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        boolean activeOnly,
        int page,
        int size,
        String sortBy,
        String sortDirection
) {
    public ProductSearchQuery {
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";
        if (sortDirection == null || sortDirection.isBlank()) sortDirection = "desc";
    }
}
