package com.ecommerce.notification.application.port.out;

import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationType;

import java.util.List;
import java.util.UUID;

/** Persistence boundary for the notification audit log. */
public interface NotificationRepositoryPort {

    boolean existsByReferenceIdAndType(UUID referenceId, NotificationType type);

    List<Notification> findByReferenceId(UUID referenceId);

    Notification save(Notification notification);
}
