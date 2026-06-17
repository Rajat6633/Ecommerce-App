package com.ecommerce.kyc.domain.exception;

import java.util.UUID;

/** Raised when no KYC case exists for the requested user. */
public class KycCaseNotFoundException extends KycException {
    public KycCaseNotFoundException(UUID userId) {
        super("No KYC case found for user " + userId);
    }
}
