package com.ecommerce.kyc.application.port.in;

import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;

import java.util.List;
import java.util.UUID;

/** Read-side queries for the self-status and officer endpoints. */
public interface KycQueryUseCase {

    KycCase getByUserId(UUID userId);

    List<KycCase> getByStatus(KycStatus status, int page, int size);
}
