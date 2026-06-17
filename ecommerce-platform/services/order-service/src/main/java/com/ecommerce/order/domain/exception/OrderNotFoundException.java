package com.ecommerce.order.domain.exception;

/** -> HTTP 404. */
public class OrderNotFoundException extends OrderException {
    public OrderNotFoundException(String id) {
        super("Order not found: " + id);
    }
}
