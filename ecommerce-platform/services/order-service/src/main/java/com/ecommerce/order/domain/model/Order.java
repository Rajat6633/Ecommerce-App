package com.ecommerce.order.domain.model;

import com.ecommerce.order.domain.exception.InvalidOrderStateException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Order aggregate (immutable). Created in PENDING; the saga drives it through
 * status transitions guarded by {@link OrderStatus#canTransitionTo}.
 */
public record Order(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<OrderItem> items,
        Long version,
        Instant createdAt,
        Instant updatedAt
) {
    public Order {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static Order create(UUID id, UUID userId, String currency, List<OrderItem> items, Instant now) {
        BigDecimal total = items.stream().map(OrderItem::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Order(id, userId, OrderStatus.PENDING, total, currency, items, null, now, now);
    }

    public boolean canTransitionTo(OrderStatus target) {
        return status.canTransitionTo(target);
    }

    /** Transition to {@code target}; idempotent if already there; throws if illegal. */
    public Order transitionTo(OrderStatus target, Instant now) {
        if (status == target) {
            return this;
        }
        if (!status.canTransitionTo(target)) {
            throw new InvalidOrderStateException(id, status, target);
        }
        return new Order(id, userId, target, totalAmount, currency, items, version, createdAt, now);
    }
}
