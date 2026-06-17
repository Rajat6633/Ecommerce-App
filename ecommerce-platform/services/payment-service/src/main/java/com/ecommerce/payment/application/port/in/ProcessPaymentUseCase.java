package com.ecommerce.payment.application.port.in;

import com.ecommerce.payment.domain.model.Payment;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Processes a payment when stock is reserved. Idempotent per order/event;
 * returns an outcome so the caller (Kafka consumer) can publish the matching
 * event after the transaction commits.
 */
public interface ProcessPaymentUseCase {

    PaymentOutcome process(UUID eventId, UUID orderId, BigDecimal amount, String currency);

    enum Status { COMPLETED, FAILED, ALREADY_PROCESSED }

    record PaymentOutcome(Status status, Payment payment, String failureReason) {
        public static PaymentOutcome completed(Payment p) {
            return new PaymentOutcome(Status.COMPLETED, p, null);
        }
        public static PaymentOutcome failed(Payment p, String reason) {
            return new PaymentOutcome(Status.FAILED, p, reason);
        }
        public static PaymentOutcome alreadyProcessed() {
            return new PaymentOutcome(Status.ALREADY_PROCESSED, null, null);
        }
    }
}
