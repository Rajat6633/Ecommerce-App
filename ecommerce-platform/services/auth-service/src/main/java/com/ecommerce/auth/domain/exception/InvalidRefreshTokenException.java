package com.ecommerce.auth.domain.exception;

/** Thrown when a refresh token is unknown, expired, or revoked. -> HTTP 401. */
public class InvalidRefreshTokenException extends AuthException {
    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}
