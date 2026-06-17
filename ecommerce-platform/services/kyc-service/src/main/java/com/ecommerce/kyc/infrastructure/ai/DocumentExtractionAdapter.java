package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.application.port.out.DocumentExtractionPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * Multimodal ID-document extraction via Claude vision (Spring AI {@link ChatClient}).
 * The model returns the structured fields as JSON, which we map to the domain
 * record. Resilience4j-wrapped; fails closed to a low-confidence result so the
 * case is reviewed rather than auto-cleared.
 */
@Component
public class DocumentExtractionAdapter implements DocumentExtractionPort {

    private static final Logger log = LoggerFactory.getLogger(DocumentExtractionAdapter.class);
    private static final String CB = "kyc-ai";
    private static final String PROMPT = """
            You are a KYC document reader. Extract the following fields from this ID document image
            and respond with ONLY a compact JSON object, no prose:
            {"fullName":..,"documentNumber":..,"dateOfBirth":..,"expiry":..,"nationality":..}
            Use null for any field you cannot read with confidence.
            """;

    private final ChatClient chatClient;
    private final DocumentJsonParser parser;
    private final Timer latency;

    public DocumentExtractionAdapter(ChatModel chatModel, MeterRegistry meterRegistry) {
        this.chatClient = ChatClient.create(chatModel);
        this.parser = new DocumentJsonParser();
        this.latency = Timer.builder("kyc_ai_call_latency").tag("op", "extract").register(meterRegistry);
        meterRegistry.counter("kyc_ai_calls_total", "op", "extract");
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "extractFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public ExtractedDocument extract(byte[] imageBytes, String mediaType) {
        org.springframework.util.MimeType mime = safeMimeType(mediaType);
        return latency.record(() -> {
            String json = chatClient.prompt()
                    .user(u -> u.text(PROMPT)
                            .media(mime, new ByteArrayResource(imageBytes)))
                    .call()
                    .content();
            return parser.parse(json);
        });
    }

    @SuppressWarnings("unused")
    ExtractedDocument extractFallback(byte[] imageBytes, String mediaType, Throwable t) {
        log.warn("Document extraction unavailable ({}) — failing closed to low confidence", t.toString());
        return ExtractedDocument.lowConfidence();
    }

    /**
     * Defense-in-depth: the controller already validates uploads to an
     * allow-listed type, but never let attacker-controlled junk reach the MIME
     * parser unguarded — fall back to PNG on blank/unparseable input.
     */
    private static org.springframework.util.MimeType safeMimeType(String mediaType) {
        if (mediaType == null || mediaType.isBlank()) {
            return MimeTypeUtils.IMAGE_PNG;
        }
        try {
            return MimeTypeUtils.parseMimeType(mediaType);
        } catch (RuntimeException e) {
            log.warn("Unparseable media type '{}' — defaulting to image/png", mediaType);
            return MimeTypeUtils.IMAGE_PNG;
        }
    }
}
