package com.ecommerce.payment.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payment aggregate (immutable). One payment per order. */
public record Payment(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
    public static Payment initiate(UUID id, UUID orderId, BigDecimal amount, String currency, Instant now) {
        return new Payment(id, orderId, amount, currency, PaymentStatus.INITIATED, null, now, now);
    }

    public Payment completed(Instant now) {
        return new Payment(id, orderId, amount, currency, PaymentStatus.COMPLETED, null, createdAt, now);
    }

    public Payment failed(String reason, Instant now) {
        return new Payment(id, orderId, amount, currency, PaymentStatus.FAILED, reason, createdAt, now);
    }
}
