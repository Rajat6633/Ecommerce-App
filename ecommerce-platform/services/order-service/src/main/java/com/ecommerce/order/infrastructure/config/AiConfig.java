package com.ecommerce.order.infrastructure.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Offline AI wiring for the support chatbot (D4). Provides a deterministic stub
 * {@link ChatModel} so the test suite ({@code mvn test}) runs with no API key, no
 * Ollama, and no model download. Mirrors kyc-service's AiConfig.
 *
 * <p><b>ChatModel — exactly one bean per profile.</b> Both the Ollama and the
 * Anthropic starters are on the classpath, and each vendor's auto-configured
 * {@code ChatModel} is gated by {@code spring.ai.<vendor>.chat.enabled}
 * (default {@code true}) <em>and</em> {@code @ConditionalOnMissingBean}. To make
 * selection deterministic we (a) default <em>both</em> vendor chat auto-configs
 * to {@code false} in {@code application.yml} and re-enable exactly one per
 * profile, and (b) register this stub {@link ChatModel} only under {@code test}:
 * <ul>
 *   <li>{@code test}  → stub (both vendor chat auto-configs off; stub is the only ChatModel)</li>
 *   <li>{@code local} → Ollama  ({@code spring.ai.ollama.chat.enabled=true})</li>
 *   <li>{@code docker}→ Ollama  ({@code spring.ai.ollama.chat.enabled=true})</li>
 *   <li>{@code k8s}   → Ollama  ({@code spring.ai.ollama.chat.enabled=true})</li>
 *   <li>{@code cloud} → Anthropic ({@code spring.ai.anthropic.chat.enabled=true})</li>
 * </ul>
 *
 * <p>No embeddings / no pgvector here — the support chatbot's retrieval is over
 * the relational order history, not vectors.
 */
@Configuration
public class AiConfig {

    /**
     * Deterministic offline chat model — {@code test} only. Under {@code local}/
     * {@code docker}/{@code k8s} the Ollama auto-config supplies the ChatModel and
     * under {@code cloud} the Anthropic auto-config does; this stub is absent there
     * so it can never collide with them.
     */
    @Bean
    @Profile("test")
    public ChatModel chatModel() {
        return new StubChatModel();
    }
}
