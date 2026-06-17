package com.ecommerce.product.application.model;

import java.util.UUID;

/**
 * A vector-store hit: the matched product id and its similarity score in
 * {@code [0,1]} (1 = identical). Framework-agnostic so the application layer
 * never sees Spring AI types.
 */
public record ScoredProductId(UUID productId, double score) {
}
