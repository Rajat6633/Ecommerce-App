package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.InventoryReleasedEvent;
import com.ecommerce.common.events.payload.InventoryReservationFailedEvent;
import com.ecommerce.common.events.payload.InventoryReservedEvent;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservedItem;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Publishes inventory saga events. Resilience4j retries/short-circuits transient
 * broker failures. Trace context propagates via Micrometer's Kafka header
 * instrumentation, so the envelope trace fields may be left null here.
 */
@Component
public class InventoryEventPublisherAdapter implements InventoryEventPublisherPort {

    private static final String CB = "kafka-publisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InventoryEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishReserved(UUID orderId, BigDecimal amount, String currency, List<ReservedItem> items) {
        var reservations = items.stream()
                .map(i -> new InventoryReservedEvent.ReservedLine(i.productId(), i.quantity()))
                .toList();
        send(Topics.INVENTORY_RESERVED, orderId,
                EventEnvelope.create(Topics.INVENTORY_RESERVED, null, null,
                        new InventoryReservedEvent(orderId, amount, currency, reservations)));
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishReservationFailed(UUID orderId, String reason) {
        send(Topics.INVENTORY_RESERVATION_FAILED, orderId,
                EventEnvelope.create(Topics.INVENTORY_RESERVATION_FAILED, null, null,
                        new InventoryReservationFailedEvent(orderId, reason)));
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishReleased(UUID orderId) {
        send(Topics.INVENTORY_RELEASED, orderId,
                EventEnvelope.create(Topics.INVENTORY_RELEASED, null, null,
                        new InventoryReleasedEvent(orderId)));
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload);
    }
}
