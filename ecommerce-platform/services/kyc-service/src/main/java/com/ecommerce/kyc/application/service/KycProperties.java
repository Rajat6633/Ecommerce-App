package com.ecommerce.kyc.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the KYC saga. {@code riskThreshold} is the embedding-similarity
 * cut-off above which a screening match becomes a {@link com.ecommerce.kyc.domain.model.WatchlistHit};
 * {@code gatingEnabled} stays false until Phase 14b wires order-service (observe-only).
 */
@ConfigurationProperties(prefix = "kyc")
public record KycProperties(
        double riskThreshold,
        boolean gatingEnabled
) {
    public KycProperties {
        if (riskThreshold <= 0.0) {
            riskThreshold = 0.85;
        }
    }
}
