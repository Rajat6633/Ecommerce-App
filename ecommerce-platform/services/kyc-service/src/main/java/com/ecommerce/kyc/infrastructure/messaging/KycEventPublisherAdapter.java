package com.ecommerce.kyc.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.KycApprovedEvent;
import com.ecommerce.common.events.payload.KycRejectedEvent;
import com.ecommerce.kyc.application.port.out.KycEventPublisherPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Resilient Kafka publisher, mirroring order-service's OrderEventPublisherAdapter. */
@Component
public class KycEventPublisherAdapter implements KycEventPublisherPort {

    private static final String CB = "kafka-publisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KycEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishApproved(UUID userId) {
        send(Topics.KYC_APPROVED, userId,
                EventEnvelope.create(Topics.KYC_APPROVED, null, null, new KycApprovedEvent(userId)));
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishRejected(UUID userId, String reason) {
        send(Topics.KYC_REJECTED, userId,
                EventEnvelope.create(Topics.KYC_REJECTED, null, null, new KycRejectedEvent(userId, reason)));
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload);
    }
}
