package com.ecommerce.kyc.infrastructure.persistence;

import com.ecommerce.kyc.domain.model.KycStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface KycCaseJpaRepository extends JpaRepository<KycCaseEntity, UUID> {

    Optional<KycCaseEntity> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    List<KycCaseEntity> findByStatusOrderByCreatedAtAsc(KycStatus status, Pageable pageable);
}
