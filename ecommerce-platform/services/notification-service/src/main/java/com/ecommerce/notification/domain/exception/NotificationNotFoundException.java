package com.ecommerce.notification.domain.exception;

/** Raised when no notification exists for the requested reference. */
public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(String reference) {
        super("No notifications found for reference " + reference);
    }
}
