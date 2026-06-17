package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by kyc-service when a customer passes KYC (clean screen or officer approval). */
public record KycApprovedEvent(
        UUID userId
) {
}
