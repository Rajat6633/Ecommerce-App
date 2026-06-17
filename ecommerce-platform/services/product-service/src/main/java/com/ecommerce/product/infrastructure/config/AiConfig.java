package com.ecommerce.product.infrastructure.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Offline AI wiring for semantic search (D1) and content-based recommendations
 * (D5). Mirrors the kyc-service approach.
 *
 * <p><b>Embeddings only — no chat model.</b> Unlike kyc-service (which uses
 * Claude/Ollama chat for document extraction and risk narratives), product
 * search needs only an {@link EmbeddingModel}: text is embedded once on
 * create/update and queries are embedded at search time. The only embedding
 * starter on the classpath is Spring AI's Transformers (local ONNX) starter —
 * there is no competing vendor embedding bean and no external embeddings API
 * (so no Ollama and no API key are ever involved).
 *
 * <ul>
 *   <li>{@code test} / {@code local} → deterministic {@link StubEmbeddingModel}
 *       (registered here, so unit/integration tests need no ONNX download and no
 *       network).</li>
 *   <li>{@code docker} / {@code k8s} → Spring AI's Transformers auto-config
 *       supplies the {@link EmbeddingModel} (all-MiniLM-L6-v2, 384-dim). This stub
 *       bean is absent there, so it never collides with the real model.</li>
 * </ul>
 */
@Configuration
public class AiConfig {

    /**
     * Deterministic offline embedding model — {@code local}/{@code test}. Under
     * {@code docker}/{@code k8s} Spring AI's Transformers auto-config supplies the
     * EmbeddingModel.
     */
    @Bean
    @Profile({"local", "test"})
    public EmbeddingModel embeddingModel() {
        return new StubEmbeddingModel();
    }
}
