package com.ecommerce.cart.infrastructure.persistence;

import com.ecommerce.cart.domain.model.Cart;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "carts")
public class CartEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "cart_items", joinColumns = @JoinColumn(name = "cart_id"))
    private List<CartItemEmbeddable> items = new ArrayList<>();

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CartEntity() {
    }

    public static CartEntity fromDomain(Cart c) {
        CartEntity e = new CartEntity();
        e.id = c.id();
        e.userId = c.userId();
        e.items = c.items().stream().map(CartItemEmbeddable::fromDomain)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        e.createdAt = c.createdAt();
        e.updatedAt = c.updatedAt();
        return e;
    }

    public Cart toDomain() {
        return new Cart(id, userId, items.stream().map(CartItemEmbeddable::toDomain).toList(),
                createdAt, updatedAt);
    }
}
