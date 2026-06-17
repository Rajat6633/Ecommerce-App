package com.ecommerce.order.domain.exception;

import com.ecommerce.order.domain.model.OrderStatus;

import java.util.UUID;

/** Illegal status transition. -> HTTP 409. */
public class InvalidOrderStateException extends OrderException {
    public InvalidOrderStateException(UUID orderId, OrderStatus from, OrderStatus to) {
        super("Order %s cannot transition from %s to %s".formatted(orderId, from, to));
    }
}
