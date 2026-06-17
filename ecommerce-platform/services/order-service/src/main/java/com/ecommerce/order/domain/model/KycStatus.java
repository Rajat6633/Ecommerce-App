package com.ecommerce.order.domain.model;

/**
 * Local read-model view of a customer's KYC decision, maintained from the
 * {@code kyc.approved} / {@code kyc.rejected} Kafka stream. Only {@link #APPROVED}
 * clears the checkout gate; any other value — or an absent record — fails closed.
 */
public enum KycStatus {
    APPROVED,
    REJECTED
}
