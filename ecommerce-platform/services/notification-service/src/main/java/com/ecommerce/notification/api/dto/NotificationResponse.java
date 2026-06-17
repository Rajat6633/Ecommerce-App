package com.ecommerce.notification.api.dto;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationChannel;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.model.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID referenceId,
        NotificationChannel channel,
        String recipient,
        NotificationType type,
        NotificationStatus status,
        String payload,
        String failureReason,
        Instant createdAt,
        Instant sentAt
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.id(), n.referenceId(), n.channel(), n.recipient(),
                n.type(), n.status(), n.payload(), n.failureReason(), n.createdAt(), n.sentAt());
    }
}
