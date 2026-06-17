package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.application.service.KycProperties;
import com.ecommerce.kyc.domain.model.WatchlistHit;
import com.ecommerce.kyc.infrastructure.config.StubEmbeddingModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScreeningAdapterTest {

    private ScreeningAdapter adapterOver(VectorStore store) {
        // Low threshold so the deterministic stub embeddings clear it for an exact name.
        return new ScreeningAdapter(store, new KycProperties(0.5, false), new SimpleMeterRegistry());
    }

    @Test
    void similarityAboveThreshold_yieldsWatchlistHit() {
        SimpleVectorStore store = new SimpleVectorStore(new StubEmbeddingModel());
        store.add(List.of(new Document("Viktor Petrov",
                Map.of("source", "OFAC", "name", "Viktor Petrov"))));
        ScreeningAdapter adapter = adapterOver(store);

        List<WatchlistHit> hits = adapter.screen("Viktor Petrov");

        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).matchedName()).isEqualTo("Viktor Petrov");
        assertThat(hits.get(0).listSource()).isEqualTo("OFAC");
    }

    @Test
    void cleanName_belowThreshold_returnsNoHits() {
        SimpleVectorStore store = new SimpleVectorStore(new StubEmbeddingModel());
        store.add(List.of(new Document("Viktor Petrov", Map.of("source", "OFAC", "name", "Viktor Petrov"))));
        ScreeningAdapter adapter = new ScreeningAdapter(store, new KycProperties(0.99, false), new SimpleMeterRegistry());

        List<WatchlistHit> hits = adapter.screen("Zzqx Wmkjf");

        assertThat(hits).isEmpty();
    }

    @Test
    void failClosed_outageYieldsSyntheticHit() {
        VectorStore broken = mock(VectorStore.class);
        when(broken.similaritySearch(org.mockito.ArgumentMatchers.<org.springframework.ai.vectorstore.SearchRequest>any()))
                .thenThrow(new RuntimeException("vector store down"));
        ScreeningAdapter adapter = adapterOver(broken);

        // The fallback is what Resilience4j invokes on failure; assert its safe default.
        List<WatchlistHit> hits = adapter.screenFallback("Jane Doe", new RuntimeException("down"));

        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).listSource()).isEqualTo("SYSTEM");
        assertThat(hits.get(0).score()).isEqualTo(1.0);
    }
}
