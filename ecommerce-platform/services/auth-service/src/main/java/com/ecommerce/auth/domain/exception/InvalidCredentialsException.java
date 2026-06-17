package com.ecommerce.auth.domain.exception;

/** Thrown when login credentials do not match. -> HTTP 401. */
public class InvalidCredentialsException extends AuthException {
    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}
