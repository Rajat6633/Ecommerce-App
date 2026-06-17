package com.ecommerce.order.application.port.in;

import java.util.UUID;

/**
 * Records KYC decisions from the {@code kyc.*} stream into the local read-model.
 * Each handler is idempotent (deduped by {@code eventId}) so redelivery is safe.
 */
public interface RecordKycDecisionUseCase {

    void onKycApproved(UUID eventId, UUID userId);

    void onKycRejected(UUID eventId, UUID userId, String reason);
}
