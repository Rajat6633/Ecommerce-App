package com.ecommerce.order.domain.exception;

/** Cannot place an order from an empty cart. -> HTTP 400. */
public class EmptyCartException extends OrderException {
    public EmptyCartException() {
        super("Cannot place an order: the cart is empty");
    }
}
