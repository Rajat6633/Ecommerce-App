package com.ecommerce.notification.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderConfirmedEvent;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.NotificationCommand;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.NotificationOutcome;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventsConsumerTest {

    @Mock RecordNotificationUseCase recordNotification;
    @Captor ArgumentCaptor<NotificationCommand> commandCaptor;

    private NotificationEventsConsumer consumer;

    private void initConsumer() {
        consumer = new NotificationEventsConsumer(recordNotification);
        when(recordNotification.record(any(), any()))
                .thenReturn(NotificationOutcome.alreadyProcessed());
    }

    @Test
    void onOrderConfirmed_buildsOrderConfirmedEmailCommand() {
        initConsumer();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        EventEnvelope<OrderConfirmedEvent> envelope = EventEnvelope.create(
                Topics.ORDER_CONFIRMED, "trace", "corr", new OrderConfirmedEvent(orderId, userId));

        consumer.onOrderConfirmed(envelope);

        verify(recordNotification).record(eq(envelope.eventId()), commandCaptor.capture());
        NotificationCommand cmd = commandCaptor.getValue();
        assertThat(cmd.referenceId()).isEqualTo(orderId);
        assertThat(cmd.type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(cmd.channel()).isEqualTo(NotificationChannel.EMAIL);
        assertThat(cmd.recipient()).contains(userId.toString());
    }

    @Test
    void onPaymentCompleted_buildsPaymentCompletedEmailCommand() {
        initConsumer();
        UUID orderId = UUID.randomUUID();
        EventEnvelope<PaymentCompletedEvent> envelope = EventEnvelope.create(
                Topics.PAYMENT_COMPLETED, "trace", "corr",
                new PaymentCompletedEvent(orderId, UUID.randomUUID(), new BigDecimal("39.98"), "USD"));

        consumer.onPaymentCompleted(envelope);

        verify(recordNotification).record(eq(envelope.eventId()), commandCaptor.capture());
        NotificationCommand cmd = commandCaptor.getValue();
        assertThat(cmd.referenceId()).isEqualTo(orderId);
        assertThat(cmd.type()).isEqualTo(NotificationType.PAYMENT_COMPLETED);
        assertThat(cmd.message()).contains("39.98").contains("USD");
    }
}
