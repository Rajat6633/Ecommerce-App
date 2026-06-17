package com.ecommerce.cart.api.dto;

import com.ecommerce.cart.domain.model.Cart;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID userId,
        List<Item> items,
        BigDecimal totalAmount,
        Instant updatedAt
) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static CartResponse from(Cart cart) {
        List<Item> items = cart.items().stream()
                .map(i -> new Item(i.productId(), i.quantity(), i.unitPrice(), i.lineTotal()))
                .toList();
        return new CartResponse(cart.userId(), items, cart.total(), cart.updatedAt());
    }
}
