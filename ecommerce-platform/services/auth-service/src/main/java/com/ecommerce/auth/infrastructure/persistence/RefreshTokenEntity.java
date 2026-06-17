package com.ecommerce.auth.infrastructure.persistence;

import com.ecommerce.auth.domain.model.RefreshToken;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** JPA mapping for refresh tokens (only the hash is persisted). */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, unique = true)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked;

    @Column(nullable = false)
    private Instant createdAt;

    protected RefreshTokenEntity() { // for JPA
    }

    public static RefreshTokenEntity fromDomain(RefreshToken t) {
        RefreshTokenEntity e = new RefreshTokenEntity();
        e.id = t.id();
        e.userId = t.userId();
        e.tokenHash = t.tokenHash();
        e.expiresAt = t.expiresAt();
        e.revoked = t.revoked();
        e.createdAt = t.createdAt();
        return e;
    }

    public RefreshToken toDomain() {
        return new RefreshToken(id, userId, tokenHash, expiresAt, revoked, createdAt);
    }
}
