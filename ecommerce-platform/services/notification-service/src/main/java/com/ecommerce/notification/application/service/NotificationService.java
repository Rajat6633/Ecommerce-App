package com.ecommerce.notification.application.service;

import com.ecommerce.notification.application.port.in.NotificationQueryUseCase;
import com.ecommerce.notification.application.port.in.RecordNotificationUseCase;
import com.ecommerce.notification.application.port.out.NotificationRepositoryPort;
import com.ecommerce.notification.application.port.out.NotificationSenderPort;
import com.ecommerce.notification.application.port.out.NotificationSenderPort.SendResult;
import com.ecommerce.notification.application.port.out.ProcessedEventPort;
import com.ecommerce.notification.domain.model.Notification;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

/**
 * Dispatches a notification and persists its audit record. Idempotent per event
 * (processed-events ledger) and per (referenceId, type) so a redelivered or
 * duplicate domain event never sends a second email.
 */
@Service
public class NotificationService implements RecordNotificationUseCase, NotificationQueryUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepositoryPort notificationRepository;
    private final NotificationSenderPort sender;
    private final ProcessedEventPort processedEvents;
    private final Clock clock;
    private final Counter notificationsSent;
    private final Counter notificationsFailed;

    public NotificationService(NotificationRepositoryPort notificationRepository,
                               NotificationSenderPort sender,
                               ProcessedEventPort processedEvents,
                               Clock clock,
                               MeterRegistry meterRegistry) {
        this.notificationRepository = notificationRepository;
        this.sender = sender;
        this.processedEvents = processedEvents;
        this.clock = clock;
        this.notificationsSent = Counter.builder("notifications_sent_total")
                .description("Total notifications delivered successfully").register(meterRegistry);
        this.notificationsFailed = Counter.builder("notifications_failed_total")
                .description("Total notifications that failed to deliver").register(meterRegistry);
    }

    @Override
    @Transactional
    public NotificationOutcome record(UUID eventId, NotificationCommand cmd) {
        if (!processedEvents.firstSeen(eventId)
                || notificationRepository.existsByReferenceIdAndType(cmd.referenceId(), cmd.type())) {
            log.info("Notification for reference {} type {} (event {}) already processed — skipping",
                    cmd.referenceId(), cmd.type(), eventId);
            return NotificationOutcome.alreadyProcessed();
        }

        Notification notification = Notification.create(
                UUID.randomUUID(), cmd.referenceId(), cmd.channel(), cmd.recipient(),
                cmd.type(), cmd.message(), clock.instant());

        SendResult result = sender.send(notification);

        if (result.delivered()) {
            Notification sent = notificationRepository.save(notification.sent(clock.instant()));
            notificationsSent.increment();
            log.info("Notification sent reference={} type={} recipient={}",
                    sent.referenceId(), sent.type(), sent.recipient());
            return NotificationOutcome.recorded(sent);
        }

        Notification failed = notificationRepository.save(
                notification.failed(result.failureReason(), clock.instant()));
        notificationsFailed.increment();
        log.warn("Notification delivery failed reference={} type={} reason={}",
                failed.referenceId(), failed.type(), result.failureReason());
        return NotificationOutcome.recorded(failed);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getByReference(UUID referenceId) {
        return notificationRepository.findByReferenceId(referenceId);
    }
}
