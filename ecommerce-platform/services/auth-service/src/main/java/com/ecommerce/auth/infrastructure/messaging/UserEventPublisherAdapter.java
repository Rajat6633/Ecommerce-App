package com.ecommerce.auth.infrastructure.messaging;

import com.ecommerce.auth.application.port.out.UserEventPublisherPort;
import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.UserRegisteredEvent;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * Publishes {@code user.registered} to Kafka after a successful registration.
 *
 * <p><strong>Commit-before-publish:</strong> when invoked inside an active
 * transaction the send is deferred until {@code AFTER_COMMIT}, so the event is
 * only emitted once the user is durably persisted (no phantom events on
 * rollback). When no transaction is active the send happens inline.
 *
 * <p>Resilience4j retries / short-circuits transient broker faults exactly like
 * the order/payment/inventory publishers (uniform {@code kafka-publisher}
 * instance). The {@code fallback} logs and swallows so a publish failure can
 * never fail or roll back the user-facing registration.
 */
@Component
public class UserEventPublisherAdapter implements UserEventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(UserEventPublisherAdapter.class);
    private static final String CB = "kafka-publisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publishUserRegistered(UUID userId, String email) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublishUserRegistered(userId, email);
                }
            });
        } else {
            doPublishUserRegistered(userId, email);
        }
    }

    @CircuitBreaker(name = CB, fallbackMethod = "publishFallback")
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void doPublishUserRegistered(UUID userId, String email) {
        var payload = new UserRegisteredEvent(userId, email);
        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(),
                EventEnvelope.create(Topics.USER_REGISTERED, null, null, payload));
    }

    /**
     * Resilience4j fallback — invoked when the publish is short-circuited,
     * rate-limited, bulkhead-rejected, exhausts retries, or otherwise throws.
     * Logs and swallows: a publish failure must never break registration.
     */
    @SuppressWarnings("unused")
    private void publishFallback(UUID userId, String email, Throwable t) {
        log.error("Failed to publish user.registered userId={} — swallowing so registration is unaffected: {}",
                userId, t.toString());
    }
}
