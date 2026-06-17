package com.ecommerce.auth.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * User aggregate (pure domain — no framework dependencies).
 * Immutable; password is stored already hashed.
 */
public record User(
        UUID id,
        String email,
        String passwordHash,
        String fullName,
        boolean enabled,
        Set<Role> roles,
        Instant createdAt,
        Instant updatedAt
) {
    public User {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    /** Factory for a freshly registered customer. */
    public static User newCustomer(UUID id, String email, String passwordHash, String fullName, Instant now) {
        return new User(id, email, passwordHash, fullName, true, Set.of(Role.CUSTOMER), now, now);
    }
}
