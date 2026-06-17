package com.ecommerce.notification.infrastructure.persistence;

import com.ecommerce.notification.application.port.out.NotificationRepositoryPort;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class NotificationPersistenceAdapter implements NotificationRepositoryPort {

    private final NotificationJpaRepository repository;

    public NotificationPersistenceAdapter(NotificationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByReferenceIdAndType(UUID referenceId, NotificationType type) {
        return repository.existsByReferenceIdAndType(referenceId, type);
    }

    @Override
    public List<Notification> findByReferenceId(UUID referenceId) {
        return repository.findByReferenceIdOrderByCreatedAtAsc(referenceId).stream()
                .map(NotificationEntity::toDomain)
                .toList();
    }

    @Override
    public Notification save(Notification notification) {
        return repository.save(NotificationEntity.fromDomain(notification)).toDomain();
    }
}
