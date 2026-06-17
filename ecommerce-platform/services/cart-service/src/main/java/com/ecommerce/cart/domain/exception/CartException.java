package com.ecommerce.cart.domain.exception;

public abstract class CartException extends RuntimeException {
    protected CartException(String message) {
        super(message);
    }
}
