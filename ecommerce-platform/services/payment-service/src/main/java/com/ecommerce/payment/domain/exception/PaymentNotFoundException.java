package com.ecommerce.payment.domain.exception;

/** -> HTTP 404. */
public class PaymentNotFoundException extends PaymentException {
    public PaymentNotFoundException(String orderId) {
        super("No payment found for order: " + orderId);
    }
}
