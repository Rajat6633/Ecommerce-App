package com.ecommerce.auth.domain.exception;

/** Base type for all auth domain errors. */
public abstract class AuthException extends RuntimeException {
    protected AuthException(String message) {
        super(message);
    }
}
