package com.ecommerce.notification.application.port.in;

import com.ecommerce.notification.domain.model.Notification;

import java.util.List;
import java.util.UUID;

/** Read side — query the notification audit log. */
public interface NotificationQueryUseCase {

    List<Notification> getByReference(UUID referenceId);
}
