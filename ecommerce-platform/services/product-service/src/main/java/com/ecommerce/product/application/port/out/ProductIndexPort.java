package com.ecommerce.product.application.port.out;

import com.ecommerce.product.application.model.ScoredProductId;
import com.ecommerce.product.domain.model.Product;

import java.util.List;
import java.util.UUID;

/**
 * Outbound port for the product vector index (semantic search + recommendations).
 * The implementation embeds {@code name + description} and stores the vector keyed
 * by product id. Kept free of Spring AI types so the application/domain layers stay
 * framework-agnostic.
 *
 * <p>Indexing is <em>fail-soft</em>: implementations must not let an indexing
 * failure propagate and break product CRUD.
 */
public interface ProductIndexPort {

    /** Embeds {@code name + description} and upserts the vector (id = productId). */
    void index(Product product);

    /** Removes a product's vector from the index (best-effort). */
    void remove(UUID productId);

    /**
     * Embeds the free-text query and returns the nearest product ids with scores,
     * highest similarity first. {@code topK} caps the result size.
     */
    List<ScoredProductId> searchByText(String query, int topK);

    /**
     * Returns the product ids most similar to the given product, highest
     * similarity first, EXCLUDING {@code productId} itself. {@code text} is the
     * product's {@code name + description} (the same text it was indexed under),
     * passed in so the adapter need not read its own stored vectors back.
     */
    List<ScoredProductId> findSimilar(UUID productId, String text, int topK);
}
