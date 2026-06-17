package com.ecommerce.order.domain.exception;

/** cart-service could not be reached when placing an order. -> HTTP 503. */
public class CartServiceUnavailableException extends OrderException {
    public CartServiceUnavailableException() {
        super("Cart service is currently unavailable; please retry shortly");
    }
}
