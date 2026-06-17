package com.ecommerce.order.domain.model;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Order lifecycle states + allowed transitions. Transitions are lenient about
 * cross-topic event ordering (e.g. PAID may arrive from PENDING if the
 * inventory.reserved event hasn't been processed yet) but never resurrect a
 * terminal order.
 */
public enum OrderStatus {
    PENDING,
    INVENTORY_RESERVED,
    PAID,
    CONFIRMED,
    REJECTED,
    PAYMENT_FAILED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = Map.of(
            PENDING, EnumSet.of(INVENTORY_RESERVED, PAID, REJECTED, PAYMENT_FAILED, CANCELLED),
            INVENTORY_RESERVED, EnumSet.of(PAID, PAYMENT_FAILED, REJECTED, CANCELLED),
            PAID, EnumSet.of(CONFIRMED),
            CONFIRMED, EnumSet.noneOf(OrderStatus.class),
            REJECTED, EnumSet.noneOf(OrderStatus.class),
            PAYMENT_FAILED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.noneOf(OrderStatus.class));

    public boolean canTransitionTo(OrderStatus target) {
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isTerminal() {
        return ALLOWED.getOrDefault(this, Set.of()).isEmpty();
    }
}
