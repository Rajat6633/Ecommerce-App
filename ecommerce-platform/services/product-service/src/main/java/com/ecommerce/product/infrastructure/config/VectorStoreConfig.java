package com.ecommerce.product.infrastructure.config;

import com.ecommerce.product.application.port.in.ReindexUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Vector store wiring for product semantic search / recommendations. Mirrors
 * the kyc-service approach.
 *
 * <p>Under {@code local}/{@code test} we use an in-memory {@link SimpleVectorStore}
 * over the stub embedding model — no Postgres/pgvector required for tests. Under
 * {@code docker}/{@code k8s} Spring AI's pgvector auto-configuration owns the
 * {@link VectorStore} bean (table {@code vector_store} in {@code product_db},
 * created by Flyway).
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Bean
    @Profile({"local", "test"})
    public VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    /**
     * Rebuilds the in-memory index on startup under {@code local} so the dev
     * SimpleVectorStore (which does not persist) reflects the existing catalog.
     * Fail-soft: a rebuild failure never blocks startup. Not registered under
     * {@code test} (ITs index their own fixtures) or {@code docker}/{@code k8s}
     * (pgvector persists across restarts; use the admin reindex endpoint instead).
     */
    @Bean
    @Profile("local")
    public ApplicationRunner reindexOnStartup(ReindexUseCase reindex) {
        return args -> {
            try {
                reindex.reindexAll();
            } catch (RuntimeException e) {
                log.warn("Startup product reindex failed (continuing): {}", e.toString());
            }
        };
    }
}
