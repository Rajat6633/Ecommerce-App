package com.ecommerce.notification.application.port.out;

import com.ecommerce.notification.domain.model.Notification;

/** Outbound dispatch boundary (email today). Implementations must not throw on
 *  delivery failure — they report it via {@link SendResult} so the audit log can
 *  persist a FAILED row. */
public interface NotificationSenderPort {

    SendResult send(Notification notification);

    /** Outcome of a single delivery attempt. */
    record SendResult(boolean delivered, String failureReason) {

        public static SendResult success() {
            return new SendResult(true, null);
        }

        public static SendResult failure(String reason) {
            return new SendResult(false, reason);
        }
    }
}
