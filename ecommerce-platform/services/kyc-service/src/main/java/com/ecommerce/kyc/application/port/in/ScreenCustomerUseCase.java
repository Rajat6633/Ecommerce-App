package com.ecommerce.kyc.application.port.in;

import com.ecommerce.kyc.domain.model.KycCase;

import java.util.UUID;

/** Driven by the user.registered consumer: open a case and screen the customer. */
public interface ScreenCustomerUseCase {

    /**
     * Idempotent per {@code eventId} and per {@code userId}. Creates the case
     * (PENDING), screens, and resolves to APPROVED (clean) or MANUAL_REVIEW
     * (hit / fail-closed). Publishes kyc.approved only on a clean auto-approve.
     */
    ScreeningOutcome onUserRegistered(UUID eventId, UUID userId, String email);

    enum Status { SCREENED, ALREADY_PROCESSED }

    /** {@code kycCase} is null when the event was a duplicate. */
    record ScreeningOutcome(Status status, KycCase kycCase) {
        public static ScreeningOutcome screened(KycCase kycCase) {
            return new ScreeningOutcome(Status.SCREENED, kycCase);
        }

        public static ScreeningOutcome alreadyProcessed() {
            return new ScreeningOutcome(Status.ALREADY_PROCESSED, null);
        }
    }
}
