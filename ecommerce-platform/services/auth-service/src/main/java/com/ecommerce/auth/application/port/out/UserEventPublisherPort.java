package com.ecommerce.auth.application.port.out;

import java.util.UUID;

/**
 * Outbound port for emitting user lifecycle events. The application layer
 * depends on this interface only — it stays free of Spring/Kafka types. The
 * infrastructure adapter is responsible for publishing post-commit and for
 * swallowing transient publish failures so they never break registration.
 */
public interface UserEventPublisherPort {

    /**
     * Publish a {@code user.registered} event for the given newly registered
     * user. Implementations must defer the actual send until after the current
     * transaction commits (commit-before-publish).
     */
    void publishUserRegistered(UUID userId, String email);
}
