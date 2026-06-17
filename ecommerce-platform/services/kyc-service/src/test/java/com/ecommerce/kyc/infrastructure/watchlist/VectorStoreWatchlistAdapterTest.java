package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.domain.model.WatchlistEntry;
import com.ecommerce.kyc.infrastructure.config.StubEmbeddingModel;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class VectorStoreWatchlistAdapterTest {

    private static WatchlistEntry entry(String id, String name, List<String> aliases) {
        return new WatchlistEntry("OFAC", id, name, aliases, "individual");
    }

    @Test
    void embedsAndStoresOneDocumentPerScreenableName() {
        // Spy the embedding model so we can assert it was invoked per name.
        EmbeddingModel embedding = spy(new StubEmbeddingModel());
        SimpleVectorStore store = new SimpleVectorStore(embedding);
        VectorStoreWatchlistAdapter adapter = new VectorStoreWatchlistAdapter(store);

        // 2 entries: one with 2 aliases (3 names), one with none (1 name) => 4 docs.
        adapter.upsert(List.of(
                entry("173", "ABDUL AZIZ, Abu", List.of("ABU ABDUL AZIZ", "AZIZ, Abdul")),
                entry("306", "AL-NASSER, Khalid", List.of())));

        // SimpleVectorStore embeds each added document via the model.
        verify(embedding, atLeastOnce()).embed(any(Document.class));

        // The ingested primary name is now screenable in the store.
        List<Document> hits = store.similaritySearch(
                SearchRequest.query("ABDUL AZIZ, Abu").withTopK(5).withSimilarityThreshold(0.5));
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMetadata()).containsEntry("source", "OFAC");
    }

    @Test
    void upsertIsIdempotent_reingestDoesNotDuplicate() {
        VectorStore store = Mockito.mock(VectorStore.class);
        VectorStoreWatchlistAdapter adapter = new VectorStoreWatchlistAdapter(store);

        WatchlistEntry e = entry("173", "ABDUL AZIZ, Abu", List.of("ABU ABDUL AZIZ"));

        adapter.upsert(List.of(e));
        adapter.upsert(List.of(e)); // same entry again

        // Each run deletes-then-adds by the SAME deterministic ids => replace, not duplicate.
        org.mockito.ArgumentCaptor<List<Document>> addCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(store, Mockito.times(2)).add(addCaptor.capture());
        verify(store, Mockito.times(2)).delete(anyList());

        List<Document> first = addCaptor.getAllValues().get(0);
        List<Document> second = addCaptor.getAllValues().get(1);
        assertThat(second.get(0).getId()).isEqualTo(first.get(0).getId());
    }

    @Test
    void emptyInput_isNoOp() {
        VectorStore store = Mockito.mock(VectorStore.class);
        VectorStoreWatchlistAdapter adapter = new VectorStoreWatchlistAdapter(store);

        adapter.upsert(List.of());

        Mockito.verifyNoInteractions(store);
    }
}
