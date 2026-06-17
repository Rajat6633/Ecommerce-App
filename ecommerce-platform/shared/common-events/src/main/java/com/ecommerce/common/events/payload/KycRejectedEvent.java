package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by kyc-service when an officer rejects a KYC case. */
public record KycRejectedEvent(
        UUID userId,
        String reason
) {
}
