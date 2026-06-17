package com.ecommerce.product.infrastructure.ai;

import com.ecommerce.product.application.model.ScoredProductId;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.domain.model.Product;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Vector index over a Spring AI {@link VectorStore} (in-memory under
 * {@code local}/{@code test}, pgvector under {@code docker}/{@code k8s}). Embeds
 * {@code name + description} on write and runs similarity search for D1
 * (semantic search) and D5 (recommendations).
 *
 * <p>Mirrors the kyc-service ScreeningAdapter's Resilience4j wrapping. Unlike kyc
 * (which fails <em>closed</em> for compliance), product discovery <strong>fails
 * soft</strong>: indexing failures are swallowed so product CRUD never breaks, and
 * search failures return an empty result (the caller can still fall back to the
 * keyword search on {@code GET /api/products}).
 */
@Component
public class VectorStoreProductIndexAdapter implements ProductIndexPort {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreProductIndexAdapter.class);
    private static final String CB = "product-ai";
    private static final String META_PRODUCT_ID = "productId";

    private final VectorStore vectorStore;

    public VectorStoreProductIndexAdapter(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "indexFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void index(Product product) {
        String id = product.id().toString();
        // Upsert = delete-then-add so re-indexing on update never duplicates.
        try {
            vectorStore.delete(List.of(id));
        } catch (RuntimeException e) {
            // First index of a product has nothing to delete — ignore.
            log.debug("No existing vector to delete for product {} ({})", id, e.toString());
        }
        Document doc = new Document(id, textOf(product), Map.of(META_PRODUCT_ID, id));
        vectorStore.add(List.of(doc));
    }

    @SuppressWarnings("unused")
    void indexFallback(Product product, Throwable t) {
        // Fail-soft: a vector-store/embedding outage must not break product CRUD.
        log.warn("Product indexing unavailable for {} — continuing without index update: {}",
                product.id(), t.toString());
    }

    @Override
    public void remove(UUID productId) {
        try {
            vectorStore.delete(List.of(productId.toString()));
        } catch (RuntimeException e) {
            log.warn("Could not remove product {} from vector index: {}", productId, e.toString());
        }
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "searchFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public List<ScoredProductId> searchByText(String query, int topK) {
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.query(query).withTopK(topK));
        if (matches == null) {
            return List.of();
        }
        return matches.stream().map(VectorStoreProductIndexAdapter::toScored).toList();
    }

    @SuppressWarnings("unused")
    List<ScoredProductId> searchFallback(String query, int topK, Throwable t) {
        log.warn("Semantic search unavailable — returning no matches: {}", t.toString());
        return List.of();
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "similarFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public List<ScoredProductId> findSimilar(UUID productId, String text, int topK) {
        // Over-fetch by one so excluding self still leaves up to topK results.
        List<Document> matches = vectorStore.similaritySearch(
                SearchRequest.query(text).withTopK(topK + 1));
        if (matches == null) {
            return List.of();
        }
        String self = productId.toString();
        return matches.stream()
                .map(VectorStoreProductIndexAdapter::toScored)
                .filter(s -> !self.equals(s.productId().toString()))
                .limit(topK)
                .toList();
    }

    @SuppressWarnings("unused")
    List<ScoredProductId> similarFallback(UUID productId, String text, int topK, Throwable t) {
        log.warn("Recommendations unavailable for {} — returning none: {}", productId, t.toString());
        return List.of();
    }

    private static ScoredProductId toScored(Document d) {
        UUID id = UUID.fromString(String.valueOf(
                d.getMetadata().getOrDefault(META_PRODUCT_ID, d.getId())));
        return new ScoredProductId(id, scoreOf(d));
    }

    private static double scoreOf(Document d) {
        Object distance = d.getMetadata().get("distance");
        if (distance instanceof Number n) {
            // pgvector store records cosine distance in metadata; similarity = 1 - distance.
            return Math.max(0.0, Math.min(1.0, 1.0 - n.doubleValue()));
        }
        return 1.0;
    }

    private static String textOf(Product p) {
        String name = p.name() == null ? "" : p.name();
        String description = p.description() == null ? "" : p.description();
        return (name + " " + description).trim();
    }
}
