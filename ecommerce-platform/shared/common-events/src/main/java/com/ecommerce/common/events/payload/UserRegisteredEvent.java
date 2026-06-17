package com.ecommerce.common.events.payload;

import java.util.UUID;

/**
 * Emitted by auth-service when a new customer registers. Consumed by
 * kyc-service to open a KYC case. {@code email} is optional (may be null if
 * auth chooses not to propagate it).
 *
 * <p><strong>Prerequisite:</strong> auth-service does not yet publish this
 * event. This record defines the expected envelope shape; wiring auth to emit
 * it is a separate change (see docs/19 §3).
 */
public record UserRegisteredEvent(
        UUID userId,
        String email
) {
}
