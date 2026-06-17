package com.ecommerce.payment.domain.exception;

public abstract class PaymentException extends RuntimeException {
    protected PaymentException(String message) {
        super(message);
    }
}
