package com.ecommerce.order.domain.exception;

/** Checkout gated: the customer's local KYC status is not APPROVED. -> HTTP 403. */
public class KycNotApprovedException extends OrderException {
    public KycNotApprovedException() {
        super("Cannot place an order: customer KYC is not approved");
    }
}
