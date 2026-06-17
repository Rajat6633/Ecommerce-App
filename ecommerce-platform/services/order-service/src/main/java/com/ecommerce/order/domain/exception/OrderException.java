package com.ecommerce.order.domain.exception;

public abstract class OrderException extends RuntimeException {
    protected OrderException(String message) {
        super(message);
    }
}
