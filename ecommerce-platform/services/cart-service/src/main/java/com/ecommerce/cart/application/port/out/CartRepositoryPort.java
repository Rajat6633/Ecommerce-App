package com.ecommerce.cart.application.port.out;

import com.ecommerce.cart.domain.model.Cart;

import java.util.Optional;
import java.util.UUID;

public interface CartRepositoryPort {

    Optional<Cart> findByUserId(UUID userId);

    Cart save(Cart cart);
}
