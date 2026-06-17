package com.ecommerce.auth.application.port.out;

/** Outbound port for password hashing (BCrypt in infrastructure). */
public interface PasswordHasherPort {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);
}
