package com.ecommerce.product.application.port.in;

/**
 * One-shot rebuild of the product vector index — embeds every existing product
 * so semantic search / recommendations work over the current catalog (e.g. after
 * enabling the feature or recovering the store). Fail-soft per product.
 */
public interface ReindexUseCase {

    /** Re-indexes all products; returns the number successfully submitted. */
    int reindexAll();
}
