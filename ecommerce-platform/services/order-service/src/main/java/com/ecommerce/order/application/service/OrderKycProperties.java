package com.ecommerce.order.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Checkout KYC-gating tunables. When {@code gatingEnabled} is true (Phase 14b
 * default) placeOrder fails closed unless the customer's local read-model status
 * is APPROVED; when false the gate is observe-only and never blocks.
 */
@ConfigurationProperties(prefix = "order.kyc.gating")
public record OrderKycProperties(
        boolean enabled
) {
}
