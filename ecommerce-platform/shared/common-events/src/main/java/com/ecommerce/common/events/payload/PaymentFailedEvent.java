package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by payment-service on failed payment (compensation trigger). */
public record PaymentFailedEvent(
        UUID orderId,
        String reason
) {
}
