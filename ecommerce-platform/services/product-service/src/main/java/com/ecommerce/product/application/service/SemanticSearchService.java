package com.ecommerce.product.application.service;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.model.ScoredProductId;
import com.ecommerce.product.application.port.in.SemanticSearchUseCase;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.exception.ProductNotFoundException;
import com.ecommerce.product.domain.model.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * D1 (semantic search) + D5 (recommendations). Resolves scored product ids from
 * the vector index, then hydrates them from the product repository, preserving
 * the similarity ranking and dropping any ids no longer present in the catalog.
 */
@Service
public class SemanticSearchService implements SemanticSearchUseCase {

    /** Cap a page can request from the vector store, mirroring ProductSearchQuery. */
    private static final int MAX_SIZE = 100;
    /** Hard cap on recommendation count. */
    private static final int MAX_LIMIT = 50;

    private final ProductIndexPort index;
    private final ProductRepositoryPort products;

    public SemanticSearchService(ProductIndexPort index, ProductRepositoryPort products) {
        this.index = index;
        this.products = products;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<ScoredProduct> semanticSearch(String query, int page, int size) {
        int p = Math.max(0, page);
        int s = (size <= 0 || size > MAX_SIZE) ? 20 : size;

        if (query == null || query.isBlank()) {
            return new PageResult<>(List.of(), p, s, 0, 0);
        }

        // Pull enough hits to cover the requested page, then slice in memory. The
        // vector store returns at most topK ranked hits; pagination is best-effort
        // over that ranked window (semantic recall, not a full SQL count).
        int topK = Math.min(MAX_SIZE, (p + 1) * s);
        List<ScoredProduct> ranked = hydrate(index.searchByText(query, topK));

        long total = ranked.size();
        int from = Math.min(p * s, ranked.size());
        int to = Math.min(from + s, ranked.size());
        List<ScoredProduct> content = ranked.subList(from, to);
        int totalPages = (int) Math.ceil((double) total / s);
        return new PageResult<>(content, p, s, total, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoredProduct> recommendations(UUID productId, int limit) {
        int n = (limit <= 0 || limit > MAX_LIMIT) ? 10 : limit;
        Product anchor = products.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId.toString()));
        // findSimilar already excludes the anchor product itself.
        // NOTE: content-based only (product text embeddings). Co-purchase /
        // collaborative-filtering recommendations are future work — they need
        // order data, which lives in order-service (DB-per-service: no cross-service
        // joins), so they are intentionally out of scope here.
        return hydrate(index.findSimilar(productId, textOf(anchor), n));
    }

    /** Same text the index is keyed on: {@code name + description}. */
    private static String textOf(Product p) {
        String name = p.name() == null ? "" : p.name();
        String description = p.description() == null ? "" : p.description();
        return (name + " " + description).trim();
    }

    /** Resolves scored ids to products, preserving rank and skipping missing ids. */
    private List<ScoredProduct> hydrate(List<ScoredProductId> hits) {
        List<ScoredProduct> out = new ArrayList<>(hits.size());
        for (ScoredProductId hit : hits) {
            Optional<Product> product = products.findById(hit.productId());
            product.ifPresent(value -> out.add(new ScoredProduct(value, hit.score())));
        }
        return out;
    }
}
