package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.CustomerKycStatus;
import com.ecommerce.order.domain.model.KycStatus;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Local KYC read-model: queried by the checkout gate, upserted by the kyc.* consumers. */
public interface CustomerKycStatusPort {

    Optional<CustomerKycStatus> findByUserId(UUID userId);

    /** Idempotent upsert of the latest decision for a user. */
    void upsert(UUID userId, KycStatus status, Instant updatedAt);
}
