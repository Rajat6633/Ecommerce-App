package com.ecommerce.kyc.domain.model;

/**
 * Lifecycle of a KYC case. {@code MANUAL_REVIEW} is terminal-until-officer: no
 * event is published until an officer resolves it (→ APPROVED / REJECTED).
 */
public enum KycStatus {
    PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    MANUAL_REVIEW
}
