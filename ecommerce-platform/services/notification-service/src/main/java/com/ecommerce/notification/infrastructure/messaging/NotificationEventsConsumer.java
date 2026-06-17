package com.ecommerce.notification.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderConfirmedEvent;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.NotificationCommand;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Terminal saga consumer. Listens to {@code order.confirmed} and
 * {@code payment.completed}, then dispatches + audits the matching email.
 * Pure consumer — no producer, no outbound REST. Recipient addresses are
 * synthesized from the event (this platform has no user-profile lookup here).
 */
@Component
public class NotificationEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventsConsumer.class);

    private final RecordNotificationUseCase recordNotification;

    public NotificationEventsConsumer(RecordNotificationUseCase recordNotification) {
        this.recordNotification = recordNotification;
    }

    @KafkaListener(topics = Topics.ORDER_CONFIRMED)
    public void onOrderConfirmed(EventEnvelope<OrderConfirmedEvent> envelope) {
        OrderConfirmedEvent event = envelope.payload();
        log.info("Received order.confirmed orderId={} userId={}", event.orderId(), event.userId());

        NotificationCommand command = new NotificationCommand(
                event.orderId(),
                NotificationChannel.EMAIL,
                recipientForUser(event.userId()),
                NotificationType.ORDER_CONFIRMED,
                "Your order " + event.orderId() + " has been confirmed and is being prepared.");

        recordNotification.record(envelope.eventId(), command);
    }

    @KafkaListener(topics = Topics.PAYMENT_COMPLETED)
    public void onPaymentCompleted(EventEnvelope<PaymentCompletedEvent> envelope) {
        PaymentCompletedEvent event = envelope.payload();
        log.info("Received payment.completed orderId={} amount={}", event.orderId(), event.amount());

        NotificationCommand command = new NotificationCommand(
                event.orderId(),
                NotificationChannel.EMAIL,
                recipientForOrder(event.orderId()),
                NotificationType.PAYMENT_COMPLETED,
                "We received your payment of " + event.amount() + " " + event.currency()
                        + " for order " + event.orderId() + ".");

        recordNotification.record(envelope.eventId(), command);
    }

    private static String recipientForUser(UUID userId) {
        return "user-" + userId + "@example.com";
    }

    private static String recipientForOrder(UUID orderId) {
        return "order-" + orderId + "@example.com";
    }
}
