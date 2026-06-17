package com.ecommerce.kyc.domain.model;

/**
 * A normalised risk score in [0.0, 1.0] with the human-readable narrative that
 * justifies it. Immutable value object — zero framework deps.
 */
public record RiskScore(double value, String narrative) {

    public RiskScore {
        if (value < 0.0 || value > 1.0) {
            throw new IllegalArgumentException("risk score must be within [0.0, 1.0], was " + value);
        }
    }

    /** The fail-closed default used when AI/vendor calls fail (max risk, routed to review). */
    public static RiskScore failClosed(String reason) {
        return new RiskScore(1.0, "Risk could not be assessed automatically: " + reason);
    }

    public boolean isHigh(double threshold) {
        return value >= threshold;
    }
}
