package com.ecommerce.kyc.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * KYC case aggregate (immutable). One per user_id. Transitions:
 * PENDING → IN_PROGRESS → APPROVED | REJECTED | MANUAL_REVIEW. Factory and
 * transition methods are named distinctly from record accessors to avoid clashes.
 *
 * <p>Fail-closed: any unscreened-but-finished path resolves to MANUAL_REVIEW
 * rather than APPROVED. Zero framework dependencies.
 */
public record KycCase(
        UUID id,
        UUID userId,
        KycStatus status,
        RiskScore riskScore,
        String decisionReason,
        List<WatchlistHit> hits,
        String resolvedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public KycCase {
        Objects.requireNonNull(userId, "userId");
        hits = hits == null ? List.of() : List.copyOf(hits);
    }

    /** A freshly-opened case awaiting screening. */
    public static KycCase open(UUID id, UUID userId, Instant now) {
        return new KycCase(id, userId, KycStatus.PENDING, null, null, List.of(), null, now, now);
    }

    public KycCase inProgress(Instant now) {
        return new KycCase(id, userId, KycStatus.IN_PROGRESS, riskScore, decisionReason,
                hits, resolvedBy, createdAt, now);
    }

    /** Clean screen → auto-approved. */
    public KycCase approved(RiskScore score, Instant now) {
        return new KycCase(id, userId, KycStatus.APPROVED, score, "Automated screening passed",
                hits, resolvedBy, createdAt, now);
    }

    /** Watchlist hit / AI outage → parked for an officer (no event yet). */
    public KycCase manualReview(RiskScore score, List<WatchlistHit> watchlistHits, String reason, Instant now) {
        return new KycCase(id, userId, KycStatus.MANUAL_REVIEW, score, reason,
                watchlistHits, resolvedBy, createdAt, now);
    }

    /** Officer decision. {@code approve} true → APPROVED, false → REJECTED. */
    public KycCase resolvedBy(String officer, boolean approve, String reason, Instant now) {
        return new KycCase(id, userId, approve ? KycStatus.APPROVED : KycStatus.REJECTED,
                riskScore, reason, hits, officer, createdAt, now);
    }

    public boolean isTerminal() {
        return status == KycStatus.APPROVED || status == KycStatus.REJECTED;
    }
}
