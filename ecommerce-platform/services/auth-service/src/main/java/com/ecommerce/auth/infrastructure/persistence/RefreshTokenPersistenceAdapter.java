package com.ecommerce.auth.infrastructure.persistence;

import com.ecommerce.auth.application.port.out.RefreshTokenRepositoryPort;
import com.ecommerce.auth.domain.model.RefreshToken;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/** Adapts the JPA repository to {@link RefreshTokenRepositoryPort}. */
@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        return repository.save(RefreshTokenEntity.fromDomain(token)).toDomain();
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenEntity::toDomain);
    }

    @Override
    public void revoke(UUID tokenId) {
        repository.revokeById(tokenId);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        repository.revokeAllByUser(userId);
    }
}
