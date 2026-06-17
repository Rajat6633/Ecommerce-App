package com.ecommerce.cart.infrastructure.persistence;

import com.ecommerce.cart.domain.model.CartItem;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Embeddable
public class CartItemEmbeddable {

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected CartItemEmbeddable() {
    }

    static CartItemEmbeddable fromDomain(CartItem i) {
        CartItemEmbeddable e = new CartItemEmbeddable();
        e.productId = i.productId();
        e.quantity = i.quantity();
        e.unitPrice = i.unitPrice();
        e.addedAt = i.addedAt();
        return e;
    }

    CartItem toDomain() {
        return new CartItem(productId, quantity, unitPrice, addedAt);
    }
}
