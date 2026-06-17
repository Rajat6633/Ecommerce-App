package com.ecommerce.product.infrastructure.ai;

import com.ecommerce.product.application.model.ScoredProductId;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.config.StubEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VectorStoreProductIndexAdapterTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    private Product product(UUID id, String name, String description) {
        return Product.create(id, "SKU-" + id, name, description,
                new BigDecimal("1.00"), "USD", UUID.randomUUID(), NOW);
    }

    @Test
    void indexThenSearch_findsTheProduct() {
        VectorStoreProductIndexAdapter adapter =
                new VectorStoreProductIndexAdapter(new SimpleVectorStore(new StubEmbeddingModel()));
        UUID id = UUID.randomUUID();
        adapter.index(product(id, "Wireless Headphones", "bluetooth audio device"));

        List<ScoredProductId> hits = adapter.searchByText("wireless headphones", 5);

        assertThat(hits).extracting(ScoredProductId::productId).contains(id);
    }

    @Test
    void reindexingSameProduct_doesNotDuplicate() {
        VectorStoreProductIndexAdapter adapter =
                new VectorStoreProductIndexAdapter(new SimpleVectorStore(new StubEmbeddingModel()));
        UUID id = UUID.randomUUID();
        adapter.index(product(id, "Wireless Headphones", "bluetooth audio device"));
        adapter.index(product(id, "Wireless Headphones", "bluetooth audio device updated"));

        List<ScoredProductId> hits = adapter.searchByText("wireless headphones", 10);

        assertThat(hits).filteredOn(h -> h.productId().equals(id)).hasSize(1);
    }

    @Test
    void findSimilar_excludesSelf() {
        VectorStoreProductIndexAdapter adapter =
                new VectorStoreProductIndexAdapter(new SimpleVectorStore(new StubEmbeddingModel()));
        UUID self = UUID.randomUUID();
        adapter.index(product(self, "Gaming Laptop", "fast computer"));
        adapter.index(product(UUID.randomUUID(), "Gaming Laptop Air", "fast computer light"));

        List<ScoredProductId> similar = adapter.findSimilar(self, "Gaming Laptop fast computer", 5);

        assertThat(similar).extracting(ScoredProductId::productId).doesNotContain(self);
        assertThat(similar).isNotEmpty();
    }

    @Test
    void indexFailure_isSwallowed_failSoft() {
        // The fallback is what Resilience4j invokes on failure; assert it does not throw.
        VectorStore broken = mock(VectorStore.class);
        VectorStoreProductIndexAdapter adapter = new VectorStoreProductIndexAdapter(broken);

        adapter.indexFallback(product(UUID.randomUUID(), "X", "y"),
                new RuntimeException("embedding model down"));
        // no exception => fail-soft
    }

    @Test
    void searchFailure_returnsEmpty_failSoft() {
        VectorStore broken = mock(VectorStore.class);
        when(broken.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("vector store down"));
        VectorStoreProductIndexAdapter adapter = new VectorStoreProductIndexAdapter(broken);

        List<ScoredProductId> hits = adapter.searchFallback("anything", 5,
                new RuntimeException("down"));

        assertThat(hits).isEmpty();
    }
}
