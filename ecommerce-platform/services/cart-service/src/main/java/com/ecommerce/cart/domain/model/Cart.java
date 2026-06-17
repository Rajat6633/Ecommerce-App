package com.ecommerce.cart.domain.model;

import com.ecommerce.cart.domain.exception.CartItemNotFoundException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Shopping cart aggregate (immutable; mutators return a new instance). One cart
 * per user. Adding an existing product increments its quantity.
 */
public record Cart(
        UUID id,
        UUID userId,
        List<CartItem> items,
        Instant createdAt,
        Instant updatedAt
) {
    public Cart {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Cart empty(UUID id, UUID userId, Instant now) {
        return new Cart(id, userId, List.of(), now, now);
    }

    public Cart addItem(UUID productId, int quantity, BigDecimal unitPrice, Instant now) {
        List<CartItem> updated = new ArrayList<>(items);
        Optional<CartItem> existing = find(productId);
        if (existing.isPresent()) {
            CartItem cur = existing.get();
            updated.removeIf(i -> i.productId().equals(productId));
            updated.add(cur.withQuantity(cur.quantity() + quantity));
        } else {
            updated.add(new CartItem(productId, quantity, unitPrice, now));
        }
        return new Cart(id, userId, updated, createdAt, now);
    }

    public Cart updateItem(UUID productId, int quantity, Instant now) {
        if (find(productId).isEmpty()) {
            throw new CartItemNotFoundException(productId.toString());
        }
        List<CartItem> updated = new ArrayList<>(items);
        updated.removeIf(i -> i.productId().equals(productId));
        if (quantity > 0) {
            CartItem cur = find(productId).orElseThrow();
            updated.add(cur.withQuantity(quantity));
        }
        return new Cart(id, userId, updated, createdAt, now);
    }

    public Cart removeItem(UUID productId, Instant now) {
        List<CartItem> updated = new ArrayList<>(items);
        updated.removeIf(i -> i.productId().equals(productId));
        return new Cart(id, userId, updated, createdAt, now);
    }

    public Cart clear(Instant now) {
        return new Cart(id, userId, List.of(), createdAt, now);
    }

    public BigDecimal total() {
        return items.stream().map(CartItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Optional<CartItem> find(UUID productId) {
        return items.stream().filter(i -> i.productId().equals(productId)).findFirst();
    }
}
