package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderConfirmedEvent;
import com.ecommerce.common.events.payload.OrderCreatedEvent;
import com.ecommerce.common.events.payload.OrderCreatedEvent.OrderLineItem;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import com.ecommerce.order.domain.model.Order;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventPublisherAdapter implements OrderEventPublisherPort {

    private static final String CB = "kafka-publisher";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public OrderEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishOrderCreated(Order order) {
        var items = order.items().stream()
                .map(i -> new OrderLineItem(i.productId(), i.quantity(), i.unitPrice()))
                .toList();
        var payload = new OrderCreatedEvent(order.id(), order.userId(), order.currency(),
                order.totalAmount(), items);
        send(Topics.ORDER_CREATED, order.id(),
                EventEnvelope.create(Topics.ORDER_CREATED, null, null, payload));
    }

    @Override
    @CircuitBreaker(name = CB)
    @Retry(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void publishOrderConfirmed(UUID orderId, UUID userId) {
        send(Topics.ORDER_CONFIRMED, orderId,
                EventEnvelope.create(Topics.ORDER_CONFIRMED, null, null,
                        new OrderConfirmedEvent(orderId, userId)));
    }

    private void send(String topic, UUID key, Object payload) {
        kafkaTemplate.send(topic, key.toString(), payload);
    }
}
