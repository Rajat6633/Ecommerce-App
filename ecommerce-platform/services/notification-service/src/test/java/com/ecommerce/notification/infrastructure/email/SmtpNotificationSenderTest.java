package com.ecommerce.notification.infrastructure.email;

import com.ecommerce.notification.application.port.out.NotificationSenderPort.SendResult;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SmtpNotificationSenderTest {

    @Mock JavaMailSender mailSender;

    private Notification notification() {
        return Notification.create(UUID.randomUUID(), UUID.randomUUID(), NotificationChannel.EMAIL,
                "user-x@example.com", NotificationType.ORDER_CONFIRMED, "Your order is confirmed.",
                Instant.parse("2026-06-10T00:00:00Z"));
    }

    @Test
    void send_success_returnsDeliveredAndSendsExpectedMessage() {
        doNothing().when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        SmtpNotificationSender sender = new SmtpNotificationSender(mailSender, "no-reply@ecommerce.local");

        SendResult result = sender.send(notification());

        assertThat(result.delivered()).isTrue();
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("user-x@example.com");
        assertThat(sent.getFrom()).isEqualTo("no-reply@ecommerce.local");
        assertThat(sent.getSubject()).isEqualTo("Your order is confirmed");
    }

    @Test
    void send_mailException_returnsFailureNotThrown() {
        doThrow(new MailSendException("connection refused"))
                .when(mailSender).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
        SmtpNotificationSender sender = new SmtpNotificationSender(mailSender, "no-reply@ecommerce.local");

        SendResult result = sender.send(notification());

        assertThat(result.delivered()).isFalse();
        assertThat(result.failureReason()).contains("connection refused");
    }
}
