package com.ecommerce.payment.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.common.events.payload.PaymentFailedEvent;
import com.ecommerce.payment.application.port.out.PaymentEventPublisherPort;
import com.ecommerce.payment.domain.model.Payment;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PaymentEventPublisherAdapter implements PaymentEventPublisherPort {

    private static final String CB = "kafka-publisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public PaymentEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishCompleted(Payment payment) {
        var payload = new PaymentCompletedEvent(payment.orderId(), payment.id(),
                payment.amount(), payment.currency());
        send(Topics.PAYMENT_COMPLETED, payment.orderId(),
                EventEnvelope.create(Topics.PAYMENT_COMPLETED, null, null, payload));
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishFailed(UUID orderId, String reason) {
        send(Topics.PAYMENT_FAILED, orderId,
                EventEnvelope.create(Topics.PAYMENT_FAILED, null, null,
                        new PaymentFailedEvent(orderId, reason)));
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload);
    }
}
