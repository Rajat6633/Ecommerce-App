package com.ecommerce.auth.domain.exception;

/** Thrown when registering an email that already exists. -> HTTP 409. */
public class EmailAlreadyExistsException extends AuthException {
    public EmailAlreadyExistsException(String email) {
        super("Email already registered: " + email);
    }
}
