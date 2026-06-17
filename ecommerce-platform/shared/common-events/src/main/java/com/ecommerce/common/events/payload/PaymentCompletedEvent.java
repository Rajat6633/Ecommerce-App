package com.ecommerce.common.events.payload;

import java.math.BigDecimal;
import java.util.UUID;

/** Emitted by payment-service on successful payment. */
public record PaymentCompletedEvent(
        UUID orderId,
        UUID paymentId,
        BigDecimal amount,
        String currency
) {
}
