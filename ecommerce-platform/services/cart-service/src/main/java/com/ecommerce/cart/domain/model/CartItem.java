package com.ecommerce.cart.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** A line in a cart; {@code unitPrice} is a snapshot taken at add time. */
public record CartItem(
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        Instant addedAt
) {
    public CartItem {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public CartItem withQuantity(int newQuantity) {
        return new CartItem(productId, newQuantity, unitPrice, addedAt);
    }
}
