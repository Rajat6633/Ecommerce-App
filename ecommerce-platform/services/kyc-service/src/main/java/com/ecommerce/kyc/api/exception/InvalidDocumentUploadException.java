package com.ecommerce.kyc.api.exception;

/**
 * Raised when an uploaded ID document fails validation (unsupported/forged
 * content type, magic-byte mismatch, empty or oversized file). Mapped to HTTP
 * 400 by {@link GlobalExceptionHandler} — never a 500.
 */
public class InvalidDocumentUploadException extends RuntimeException {
    public InvalidDocumentUploadException(String message) {
        super(message);
    }
}
