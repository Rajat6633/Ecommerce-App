package com.ecommerce.cart.infrastructure.persistence;

import com.ecommerce.cart.application.port.out.CartRepositoryPort;
import com.ecommerce.cart.domain.model.Cart;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class CartPersistenceAdapter implements CartRepositoryPort {

    private final CartJpaRepository repository;

    public CartPersistenceAdapter(CartJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Cart> findByUserId(UUID userId) {
        return repository.findByUserId(userId).map(CartEntity::toDomain);
    }

    @Override
    public Cart save(Cart cart) {
        return repository.save(CartEntity.fromDomain(cart)).toDomain();
    }
}
