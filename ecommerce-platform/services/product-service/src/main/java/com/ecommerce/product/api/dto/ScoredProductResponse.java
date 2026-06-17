package com.ecommerce.product.api.dto;

import com.ecommerce.product.application.port.in.SemanticSearchUseCase.ScoredProduct;

/**
 * A product plus its semantic-similarity score in {@code [0,1]} (1 = identical),
 * used by D1 (search) and D5 (recommendations).
 */
public record ScoredProductResponse(ProductResponse product, double score) {
    public static ScoredProductResponse from(ScoredProduct s) {
        return new ScoredProductResponse(ProductResponse.from(s.product()), s.score());
    }
}
