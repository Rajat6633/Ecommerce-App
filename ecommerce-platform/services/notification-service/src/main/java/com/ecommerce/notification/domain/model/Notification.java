package com.ecommerce.notification.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Notification aggregate (immutable). One row per (referenceId, type) — the audit
 * record of an outbound message. Factory methods are named distinctly from the
 * record components to avoid accessor clashes.
 */
public record Notification(
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
    /** A freshly-raised notification, not yet dispatched. */
    public static Notification create(UUID id, UUID referenceId, NotificationChannel channel,
                                      String recipient, NotificationType type, String payload, Instant now) {
        return new Notification(id, referenceId, channel, recipient, type,
                NotificationStatus.PENDING, payload, null, now, null);
    }

    /** Transition to SENT, stamping the delivery time. */
    public Notification sent(Instant now) {
        return new Notification(id, referenceId, channel, recipient, type,
                NotificationStatus.SENT, payload, null, createdAt, now);
    }

    /** Transition to FAILED, recording why delivery did not succeed. */
    public Notification failed(String reason, Instant now) {
        return new Notification(id, referenceId, channel, recipient, type,
                NotificationStatus.FAILED, payload, reason, createdAt, now);
    }
}
