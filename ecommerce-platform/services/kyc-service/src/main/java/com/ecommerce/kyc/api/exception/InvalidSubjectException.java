package com.ecommerce.kyc.api.exception;

/**
 * Raised when the authenticated principal's subject is not a valid UUID (a
 * malformed token), so the caller's identity cannot be resolved. Mapped to HTTP
 * 400 by {@link GlobalExceptionHandler} rather than surfacing as a 500.
 */
public class InvalidSubjectException extends RuntimeException {
    public InvalidSubjectException(String subject) {
        super("Authenticated subject is not a valid user id: " + subject);
    }
}
