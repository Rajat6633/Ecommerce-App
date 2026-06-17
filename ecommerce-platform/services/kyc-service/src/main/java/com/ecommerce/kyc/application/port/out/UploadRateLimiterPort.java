package com.ecommerce.kyc.application.port.out;

import java.util.UUID;

/**
 * Per-user gate on the document-upload path (roadmap C3). Keeps the rate-limiting
 * mechanism (Resilience4j) out of the api/domain layers: callers only express the
 * intent "acquire a permit for this user", and the infrastructure adapter decides
 * how that is enforced. Complements the service-wide {@code kyc-ai} limiter — this
 * one is keyed per {@code userId} so one user cannot exhaust everyone else's budget.
 */
public interface UploadRateLimiterPort {

    /**
     * Acquire a single upload permit for {@code userId}. Implementations throw a
     * {@link RateLimitExceededException} when this user has exhausted their
     * per-minute upload budget; the api layer maps that to HTTP 429.
     */
    void acquire(UUID userId);

    /** Thrown when a user exceeds their per-user upload rate limit (→ HTTP 429). */
    class RateLimitExceededException extends RuntimeException {
        public RateLimitExceededException(String message) {
            super(message);
        }
    }
}
