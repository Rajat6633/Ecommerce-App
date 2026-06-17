package com.ecommerce.auth.application.port.out;

import com.ecommerce.auth.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for refresh-token persistence. */
public interface RefreshTokenRepositoryPort {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoke a single token (used on rotation / logout). */
    void revoke(UUID tokenId);

    /** Revoke every active token for a user (e.g. reuse detection). */
    void revokeAllForUser(UUID userId);
}
