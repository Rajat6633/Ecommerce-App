package com.ecommerce.notification.infrastructure.email;

import com.ecommerce.notification.application.port.out.NotificationSenderPort.SendResult;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingNotificationSenderTest {

    @Test
    void send_alwaysReportsSuccess() {
        Notification n = Notification.create(UUID.randomUUID(), UUID.randomUUID(), NotificationChannel.EMAIL,
                "user-x@example.com", NotificationType.PAYMENT_COMPLETED, "Payment received.",
                Instant.parse("2026-06-10T00:00:00Z"));

        SendResult result = new LoggingNotificationSender().send(n);

        assertThat(result.delivered()).isTrue();
        assertThat(result.failureReason()).isNull();
    }
}
