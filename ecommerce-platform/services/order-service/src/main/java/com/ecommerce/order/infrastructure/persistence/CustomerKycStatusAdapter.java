package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.application.port.out.CustomerKycStatusPort;
import com.ecommerce.order.domain.model.CustomerKycStatus;
import com.ecommerce.order.domain.model.KycStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerKycStatusAdapter implements CustomerKycStatusPort {

    private final CustomerKycStatusJpaRepository repository;

    public CustomerKycStatusAdapter(CustomerKycStatusJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerKycStatus> findByUserId(UUID userId) {
        return repository.findById(userId).map(CustomerKycStatusEntity::toDomain);
    }

    @Override
    @Transactional
    public void upsert(UUID userId, KycStatus status, Instant updatedAt) {
        repository.findById(userId)
                .map(existing -> {
                    existing.setStatus(status);
                    existing.setUpdatedAt(updatedAt);
                    return existing;
                })
                .or(() -> Optional.of(new CustomerKycStatusEntity(userId, status, updatedAt)))
                .ifPresent(repository::save);
    }
}
