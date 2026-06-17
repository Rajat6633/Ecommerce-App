package com.ecommerce.kyc.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Vector store wiring.
 *
 * <p>Under {@code local}/{@code test} we use an in-memory {@link SimpleVectorStore}
 * over the stub embedding model — no Postgres/pgvector required for unit tests.
 * Under {@code docker}/{@code k8s} Spring AI's pgvector auto-configuration owns
 * the {@link VectorStore} bean (table {@code vector_store} in {@code kyc_db}).
 *
 * <p>{@link WatchlistSeeder} loads a tiny fixture list into whichever store is
 * active so screening has something to match against in dev.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    @Profile({"local", "test"})
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    /**
     * Seeds a small OFAC/UN fixture list so dev screening is non-empty.
     *
     * <p>Active in {@code local}/{@code test}/{@code docker} <em>only when the
     * real scheduled ingestion is disabled</em> ({@code kyc.watchlist.ingestion.enabled=false},
     * the default). When ops turn ingestion on, the {@code OfacSdnFeedAdapter}
     * owns the watchlist and this fixture seeder stands down so it cannot
     * pollute the real list — guaranteeing offline runs still have data while
     * the two paths never both write.
     */
    @Component
    @Profile({"local", "test", "docker"})
    @ConditionalOnProperty(prefix = "kyc.watchlist.ingestion", name = "enabled", havingValue = "false", matchIfMissing = true)
    static class WatchlistSeeder {

        private static final Logger log = LoggerFactory.getLogger(WatchlistSeeder.class);

        private final VectorStore vectorStore;

        WatchlistSeeder(VectorStore vectorStore) {
            this.vectorStore = vectorStore;
        }

        @PostConstruct
        void seed() {
            List<Document> fixtures = List.of(
                    new Document("Mohammed Al-Rashid", Map.of("source", "OFAC", "name", "Mohammed Al-Rashid")),
                    new Document("Viktor Petrov", Map.of("source", "UN", "name", "Viktor Petrov")),
                    new Document("Chen Wei", Map.of("source", "EU", "name", "Chen Wei")));
            try {
                vectorStore.add(fixtures);
                log.info("Seeded {} watchlist fixture entries into the vector store", fixtures.size());
            } catch (RuntimeException e) {
                // Non-fatal in dev — a real ingestion job owns this in production.
                log.warn("Could not seed watchlist fixtures: {}", e.toString());
            }
        }
    }
}
