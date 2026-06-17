package com.ecommerce.product.application.model;

import java.util.List;

/** Framework-agnostic page result, so the application layer avoids Spring's Page. */
public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public <R> PageResult<R> map(java.util.function.Function<T, R> mapper) {
        return new PageResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
