package com.ecommerce.auth.domain.exception;

/** Thrown when a referenced user does not exist. -> HTTP 404. */
public class UserNotFoundException extends AuthException {
    public UserNotFoundException(String identifier) {
        super("User not found: " + identifier);
    }
}
