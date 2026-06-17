package com.ecommerce.auth.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token aggregate. The raw token is never stored — only its hash.
 */
public record RefreshToken(
        UUID id,
        UUID userId,
        String tokenHash,
        Instant expiresAt,
        boolean revoked,
        Instant createdAt
) {
    /** A token is usable only if not revoked and not expired. */
    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }
}
