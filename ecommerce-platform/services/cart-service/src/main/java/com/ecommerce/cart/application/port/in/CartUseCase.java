package com.ecommerce.cart.application.port.in;

import com.ecommerce.cart.domain.model.Cart;

import java.util.UUID;

/** Inbound port for cart operations. All operations are scoped to a user. */
public interface CartUseCase {

    Cart getCart(UUID userId);

    Cart addItem(UUID userId, UUID productId, int quantity);

    Cart updateItem(UUID userId, UUID productId, int quantity);

    Cart removeItem(UUID userId, UUID productId);

    Cart clearCart(UUID userId);
}
