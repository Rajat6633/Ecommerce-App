package com.ecommerce.kyc.domain.exception;

import com.ecommerce.kyc.domain.model.KycStatus;

/** Raised when an officer tries to resolve a case that is not awaiting review. */
public class InvalidCaseStateException extends KycException {
    public InvalidCaseStateException(KycStatus current) {
        super("Case cannot be resolved from state " + current + " (only MANUAL_REVIEW is resolvable)");
    }
}
