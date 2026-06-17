package com.ecommerce.product.application.service;

import com.ecommerce.product.application.model.PageResult;
import com.ecommerce.product.application.port.in.ProductSearchQuery;
import com.ecommerce.product.application.port.in.SemanticSearchUseCase.ScoredProduct;
import com.ecommerce.product.application.port.out.ProductIndexPort;
import com.ecommerce.product.application.port.out.ProductRepositoryPort;
import com.ecommerce.product.domain.model.Product;
import com.ecommerce.product.infrastructure.ai.VectorStoreProductIndexAdapter;
import com.ecommerce.product.infrastructure.config.StubEmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * D1 / D5 over a real in-memory {@link SimpleVectorStore} + deterministic
 * {@link StubEmbeddingModel} — fully offline, no network. Exercises the actual
 * {@link VectorStoreProductIndexAdapter} so similarity ranking and self-exclusion
 * are covered end to end at the application layer.
 */
class SemanticSearchServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");
    private final UUID categoryId = UUID.randomUUID();

    private final Map<UUID, Product> store = new HashMap<>();
    private ProductIndexPort index;
    private SemanticSearchService service;

    /** Minimal in-memory repository backing hydrate(); only findById/findAll used. */
    private final ProductRepositoryPort repository = new ProductRepositoryPort() {
        public boolean existsBySku(String sku) { return false; }
        public Product save(Product product) { store.put(product.id(), product); return product; }
        public Optional<Product> findById(UUID id) { return Optional.ofNullable(store.get(id)); }
        public void deleteById(UUID id) { store.remove(id); }
        public PageResult<Product> search(ProductSearchQuery query) { return new PageResult<>(List.of(), 0, 20, 0, 0); }
        public List<Product> findAll() { return List.copyOf(store.values()); }
    };

    private Product product(String name, String description) {
        Product p = Product.create(UUID.randomUUID(), "SKU-" + UUID.randomUUID(), name, description,
                new BigDecimal("9.99"), "USD", categoryId, NOW);
        store.put(p.id(), p);
        return p;
    }

    @BeforeEach
    void setUp() {
        SimpleVectorStore vectorStore = new SimpleVectorStore(new StubEmbeddingModel());
        index = new VectorStoreProductIndexAdapter(vectorStore);
        service = new SemanticSearchService(index, repository);
    }

    @Test
    void semanticSearch_ranksProductsBySimilarityToQuery() {
        Product laptop = product("Gaming Laptop", "high performance notebook computer");
        Product novel = product("Mystery Novel", "a thrilling detective story book");
        index.index(laptop);
        index.index(novel);

        PageResult<ScoredProduct> result = service.semanticSearch("gaming laptop computer", 0, 10);

        assertThat(result.content()).isNotEmpty();
        // Most similar to the query is the laptop.
        assertThat(result.content().get(0).product().id()).isEqualTo(laptop.id());
        assertThat(result.content().get(0).score()).isBetween(0.0, 1.0);
    }

    @Test
    void semanticSearch_emptyQuery_returnsEmptyPage() {
        index.index(product("Gaming Laptop", "computer"));

        PageResult<ScoredProduct> blank = service.semanticSearch("   ", 0, 10);
        PageResult<ScoredProduct> nul = service.semanticSearch(null, 0, 10);

        assertThat(blank.content()).isEmpty();
        assertThat(blank.totalElements()).isZero();
        assertThat(nul.content()).isEmpty();
    }

    @Test
    void recommendations_returnsSimilarProductsExcludingSelf() {
        Product laptop1 = product("Gaming Laptop Pro", "fast computer notebook");
        Product laptop2 = product("Gaming Laptop Air", "light computer notebook");
        Product novel = product("Mystery Novel", "detective story book");
        index.index(laptop1);
        index.index(laptop2);
        index.index(novel);

        List<ScoredProduct> recs = service.recommendations(laptop1.id(), 10);

        // Self is excluded; the other laptop ranks above the unrelated novel.
        assertThat(recs).extracting(r -> r.product().id()).doesNotContain(laptop1.id());
        assertThat(recs.get(0).product().id()).isEqualTo(laptop2.id());
    }

    @Test
    void recommendations_respectsLimit() {
        Product anchor = product("Gaming Laptop", "computer notebook");
        index.index(anchor);
        for (int i = 0; i < 5; i++) {
            index.index(product("Gaming Laptop variant " + i, "computer notebook model"));
        }

        List<ScoredProduct> recs = service.recommendations(anchor.id(), 2);

        assertThat(recs).hasSizeLessThanOrEqualTo(2);
        assertThat(recs).extracting(r -> r.product().id()).doesNotContain(anchor.id());
    }
}
