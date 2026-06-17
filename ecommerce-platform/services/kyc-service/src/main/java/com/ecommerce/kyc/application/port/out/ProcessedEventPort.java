package com.ecommerce.kyc.application.port.out;

import java.util.UUID;

/** Idempotency ledger — records an eventId the first time it is seen. */
public interface ProcessedEventPort {
    boolean firstSeen(UUID eventId);
}
