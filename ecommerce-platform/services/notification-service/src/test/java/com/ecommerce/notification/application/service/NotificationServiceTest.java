package com.ecommerce.notification.application.service;

import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.NotificationCommand;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.NotificationOutcome;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase.Status;
import com.ecommerce.notification.application.port.out.NotificationRepositoryPort;
import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.application.port.out.NotificationSenderPort.SendResult;
import com.ecommerce.notification.application.port.out.ProcessedEventPort;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.model.NotificationType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-10T00:00:00Z");

    @Mock NotificationRepositoryPort notificationRepository;
    @Mock NotificationSenderPort sender;
    @Mock ProcessedEventPort processedEvents;

    private SimpleMeterRegistry meterRegistry;
    private NotificationService service;

    private final UUID eventId = UUID.randomUUID();
    private final UUID referenceId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new NotificationService(notificationRepository, sender, processedEvents,
                Clock.fixed(NOW, ZoneOffset.UTC), meterRegistry);
    }

    private NotificationCommand command() {
        return new NotificationCommand(referenceId, NotificationChannel.EMAIL,
                "user-x@example.com", NotificationType.ORDER_CONFIRMED, "Your order is confirmed.");
    }

    @Test
    void record_delivered_marksSentAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(notificationRepository.existsByReferenceIdAndType(referenceId, NotificationType.ORDER_CONFIRMED))
                .thenReturn(false);
        when(sender.send(any())).thenReturn(SendResult.success());
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationOutcome outcome = service.record(eventId, command());

        assertThat(outcome.status()).isEqualTo(Status.RECORDED);
        assertThat(outcome.notification().status()).isEqualTo(NotificationStatus.SENT);
        assertThat(outcome.notification().sentAt()).isEqualTo(NOW);
        assertThat(meterRegistry.get("notifications_sent_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void record_deliveryFailed_marksFailedAndCounts() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(notificationRepository.existsByReferenceIdAndType(referenceId, NotificationType.ORDER_CONFIRMED))
                .thenReturn(false);
        when(sender.send(any())).thenReturn(SendResult.failure("SMTP timeout"));
        when(notificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        NotificationOutcome outcome = service.record(eventId, command());

        assertThat(outcome.status()).isEqualTo(Status.RECORDED);
        assertThat(outcome.notification().status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(outcome.notification().failureReason()).isEqualTo("SMTP timeout");
        assertThat(meterRegistry.get("notifications_failed_total").counter().count()).isEqualTo(1.0);
    }

    @Test
    void record_duplicateEvent_alreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(false);

        NotificationOutcome outcome = service.record(eventId, command());

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(sender, never()).send(any());
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void record_existingNotificationForReferenceAndType_alreadyProcessed() {
        when(processedEvents.firstSeen(eventId)).thenReturn(true);
        when(notificationRepository.existsByReferenceIdAndType(referenceId, NotificationType.ORDER_CONFIRMED))
                .thenReturn(true);

        NotificationOutcome outcome = service.record(eventId, command());

        assertThat(outcome.status()).isEqualTo(Status.ALREADY_PROCESSED);
        verify(sender, never()).send(any());
    }

    @Test
    void getByReference_returnsAudit() {
        Notification n = Notification.create(UUID.randomUUID(), referenceId, NotificationChannel.EMAIL,
                "user-x@example.com", NotificationType.PAYMENT_COMPLETED, "Payment received.", NOW).sent(NOW);
        when(notificationRepository.findByReferenceId(referenceId)).thenReturn(List.of(n));

        List<Notification> result = service.getByReference(referenceId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(NotificationStatus.SENT);
    }
}
