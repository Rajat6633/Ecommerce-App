package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.application.port.out.ScreeningPort;
import com.ecommerce.kyc.domain.model.WatchlistHit;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sanctions screening over a pgvector {@link VectorStore} seeded with embedded
 * OFAC/UN watchlist names. A similarity above the configured threshold becomes a
 * {@link WatchlistHit}. The store's embeddings come from the local Transformers
 * {@code EmbeddingModel} (configured via Spring AI), so this runs offline.
 *
 * <p><strong>Fail closed:</strong> on any AI/vector-store outage the
 * Resilience4j fallback returns a synthetic hit, so the case is routed to
 * MANUAL_REVIEW rather than silently auto-approved.
 */
@Component
public class ScreeningAdapter implements ScreeningPort {

    private static final Logger log = LoggerFactory.getLogger(ScreeningAdapter.class);
    private static final String CB = "kyc-ai";

    private final VectorStore vectorStore;
    private final double threshold;
    private final int topK;
    private final Timer latency;

    public ScreeningAdapter(VectorStore vectorStore,
                            com.ecommerce.kyc.application.service.KycProperties properties,
                            MeterRegistry meterRegistry) {
        this.vectorStore = vectorStore;
        this.threshold = properties.riskThreshold();
        this.topK = 5;
        this.latency = Timer.builder("kyc_ai_call_latency").tag("op", "screen").register(meterRegistry);
        meterRegistry.counter("kyc_ai_calls_total", "op", "screen");
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "screenFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public List<WatchlistHit> screen(String fullName) {
        return latency.record(() -> {
            List<Document> matches = vectorStore.similaritySearch(
                    SearchRequest.query(fullName).withTopK(topK).withSimilarityThreshold(threshold));
            if (matches == null) {
                return List.of();
            }
            return matches.stream()
                    .map(d -> WatchlistHit.of(
                            String.valueOf(d.getMetadata().getOrDefault("source", "UNKNOWN")),
                            String.valueOf(d.getMetadata().getOrDefault("name", d.getContent())),
                            scoreOf(d),
                            d.getContent()))
                    .toList();
        });
    }

    @SuppressWarnings("unused")
    List<WatchlistHit> screenFallback(String fullName, Throwable t) {
        // Do not log the customer name (PII); the synthetic hit still carries it for the case.
        log.warn("Screening unavailable ({}) — failing closed to MANUAL_REVIEW", t.toString());
        return List.of(WatchlistHit.of("SYSTEM", fullName, 1.0,
                "Screening could not run (fail-closed): " + t.getMessage()));
    }

    private static double scoreOf(Document d) {
        Object distance = d.getMetadata().get("distance");
        if (distance instanceof Number n) {
            // pgvector store records cosine distance in metadata; similarity = 1 - distance.
            return Math.max(0.0, Math.min(1.0, 1.0 - n.doubleValue()));
        }
        return 1.0;
    }
}
