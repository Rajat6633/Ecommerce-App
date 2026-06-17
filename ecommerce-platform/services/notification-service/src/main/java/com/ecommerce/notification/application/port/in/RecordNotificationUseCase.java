package com.ecommerce.notification.application.port.in;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationType;

import java.util.UUID;

/** Raise (dispatch + persist) a notification in response to a domain event. */
public interface RecordNotificationUseCase {

    NotificationOutcome record(UUID eventId, NotificationCommand command);

    /** What to notify and whom. */
    record NotificationCommand(
            UUID referenceId,
            NotificationChannel channel,
            String recipient,
            NotificationType type,
            String message
    ) {
    }

    enum Status {
        RECORDED,
        ALREADY_PROCESSED
    }

    /** Result of handling an event; {@code notification} is null when skipped. */
    record NotificationOutcome(Status status, Notification notification) {

        public static NotificationOutcome recorded(Notification notification) {
            return new NotificationOutcome(Status.RECORDED, notification);
        }

        public static NotificationOutcome alreadyProcessed() {
            return new NotificationOutcome(Status.ALREADY_PROCESSED, null);
        }
    }
}
