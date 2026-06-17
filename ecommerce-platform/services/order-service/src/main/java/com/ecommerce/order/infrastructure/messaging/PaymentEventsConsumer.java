package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.common.events.payload.PaymentFailedEvent;
import com.ecommerce.order.application.port.in.OrderSagaUseCase;
import com.ecommerce.order.application.port.out.OrderEventPublisherPort;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes payment outcomes. On success, the order is confirmed and (post-commit)
 * {@code order.confirmed} is published for the notification service.
 */
@Component
public class PaymentEventsConsumer {

    private final OrderSagaUseCase saga;
    private final OrderEventPublisherPort publisher;

    public PaymentEventsConsumer(OrderSagaUseCase saga, OrderEventPublisherPort publisher) {
        this.saga = saga;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onCompleted(EventEnvelope<PaymentCompletedEvent> envelope) {
        saga.onPaymentCompleted(envelope.eventId(), envelope.payload().orderId())
                .ifPresent(order -> publisher.publishOrderConfirmed(order.id(), order.userId()));
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onFailed(EventEnvelope<PaymentFailedEvent> envelope) {
        PaymentFailedEvent e = envelope.payload();
        saga.onPaymentFailed(envelope.eventId(), e.orderId(), e.reason());
    }
}
