package com.ecommerce.product.application.port.in;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.UUID;

/**
 * Inbound port for AI-powered discovery:
 * <ul>
 *   <li><b>D1 — semantic search:</b> embed a free-text query and return the
 *       nearest products by vector similarity.</li>
 *   <li><b>D5 — recommendations:</b> top-N products most similar to a given one,
 *       excluding the product itself.</li>
 * </ul>
 */
public interface SemanticSearchUseCase {

    /** D1: nearest products to {@code query}, ranked by similarity (paged). */
    PageResult<ScoredProduct> semanticSearch(String query, int page, int size);

    /** D5: top-{@code limit} products similar to {@code productId}, excluding it. */
    List<ScoredProduct> recommendations(UUID productId, int limit);

    /** A product paired with its similarity score in {@code [0,1]} (1 = identical). */
    record ScoredProduct(Product product, double score) {}
}
