package com.ecommerce.kyc.infrastructure.persistence;

import com.ecommerce.kyc.application.port.out.KycCaseRepositoryPort;
import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class KycCasePersistenceAdapter implements KycCaseRepositoryPort {

    private final KycCaseJpaRepository repository;

    public KycCasePersistenceAdapter(KycCaseJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public KycCase save(KycCase kycCase) {
        // Reuse the managed row on update so the unique (user_id) row keeps its
        // identity and the hit collection is replaced rather than duplicated.
        KycCaseEntity entity = repository.findById(kycCase.id())
                .map(existing -> {
                    existing.applyFrom(kycCase);
                    return existing;
                })
                .orElseGet(() -> KycCaseEntity.fromDomain(kycCase));
        return repository.save(entity).toDomain();
    }

    @Override
    public Optional<KycCase> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(KycCaseEntity::toDomain);
    }

    @Override
    public boolean existsByUserId(UUID userId) {
        return repository.existsByUserId(userId);
    }

    @Override
    public List<KycCase> findByStatus(KycStatus status, int page, int size) {
        return repository.findByStatusOrderByCreatedAtAsc(status, PageRequest.of(page, size)).stream()
                .map(KycCaseEntity::toDomain)
                .toList();
    }
}
