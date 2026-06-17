package com.ecommerce.kyc.application.port.out;

import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence boundary for the {@link KycCase} aggregate. */
public interface KycCaseRepositoryPort {

    KycCase save(KycCase kycCase);

    Optional<KycCase> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /** Paginated review queue, filtered by status (e.g. MANUAL_REVIEW). */
    List<KycCase> findByStatus(KycStatus status, int page, int size);
}
