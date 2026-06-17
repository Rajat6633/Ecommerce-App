package com.ecommerce.notification.infrastructure.persistence;

import com.ecommerce.notification.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, UUID> {

    boolean existsByReferenceIdAndType(UUID referenceId, NotificationType type);

    List<NotificationEntity> findByReferenceIdOrderByCreatedAtAsc(UUID referenceId);
}
