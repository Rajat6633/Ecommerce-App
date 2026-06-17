package com.ecommerce.kyc.domain.exception;

/** Base type for KYC domain failures. */
public abstract class KycException extends RuntimeException {
    protected KycException(String message) {
        super(message);
    }
}
