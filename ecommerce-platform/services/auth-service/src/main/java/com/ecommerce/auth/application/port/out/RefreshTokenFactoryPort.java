package com.ecommerce.auth.application.port.out;

/**
 * Outbound port for generating opaque refresh tokens and hashing them for
 * storage. Keeps secure-random + hashing concerns out of the domain/service.
 */
public interface RefreshTokenFactoryPort {

    /** A fresh, high-entropy opaque token (returned to the client once). */
    String generateRawToken();

    /** Deterministic hash of a raw token, for at-rest storage and lookup. */
    String hash(String rawToken);
}
