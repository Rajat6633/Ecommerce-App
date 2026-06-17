package com.ecommerce.kyc.application.port.in;

import com.ecommerce.kyc.domain.model.KycCase;

import java.util.UUID;

/** Officer resolution of a MANUAL_REVIEW case → publishes kyc.approved/kyc.rejected. */
public interface ResolveCaseUseCase {

    KycCase resolve(UUID userId, String officer, boolean approve, String reason);
}
