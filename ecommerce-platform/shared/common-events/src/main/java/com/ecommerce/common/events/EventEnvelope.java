package com.ecommerce.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard envelope wrapping every domain event. Carries identity (for consumer
 * idempotency) and correlation metadata (trace/correlation ids) alongside the
 * typed payload.
 *
 * @param <T> payload type (one of the *Event records in this package)
 */
public record EventEnvelope<T>(
        UUID eventId,
        String eventType,
        int version,
        Instant occurredAt,
        String traceId,
        String correlationId,
        T payload
) {
    /** Convenience factory stamping a fresh id + timestamp (v1). */
    public static <T> EventEnvelope<T> create(String eventType, String traceId,
                                              String correlationId, T payload) {
        return new EventEnvelope<>(UUID.randomUUID(), eventType, 1, Instant.now(),
                traceId, correlationId, payload);
    }
}
