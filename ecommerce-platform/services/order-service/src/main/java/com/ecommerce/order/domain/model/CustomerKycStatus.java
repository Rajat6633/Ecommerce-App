package com.ecommerce.order.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-model aggregate projecting a customer's latest KYC decision into order_db.
 * Updated idempotently by {@code kyc.*} consumers; read by the placeOrder gate.
 */
public record CustomerKycStatus(
        UUID userId,
        KycStatus status,
        Instant updatedAt
) {
    public boolean isApproved() {
        return status == KycStatus.APPROVED;
    }
}
