package com.ecommerce.kyc.infrastructure.persistence;

import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;
import com.ecommerce.kyc.domain.model.RiskScore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kyc_cases")
public class KycCaseEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KycStatus status;

    @Column(name = "risk_score")
    private Double riskScore;

    @Column(name = "decision_reason", length = 1024)
    private String decisionReason;

    @Column(name = "resolved_by", length = 255)
    private String resolvedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "kycCase", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("score DESC")
    private List<WatchlistHitEntity> hits = new ArrayList<>();

    protected KycCaseEntity() {
    }

    public static KycCaseEntity fromDomain(KycCase c) {
        KycCaseEntity e = new KycCaseEntity();
        e.applyFrom(c);
        return e;
    }

    /** Re-applies aggregate state onto a (possibly managed) entity, preserving identity. */
    public void applyFrom(KycCase c) {
        this.id = c.id();
        this.userId = c.userId();
        this.status = c.status();
        this.riskScore = c.riskScore() == null ? null : c.riskScore().value();
        this.decisionReason = c.decisionReason();
        this.resolvedBy = c.resolvedBy();
        this.createdAt = c.createdAt();
        this.updatedAt = c.updatedAt();
        this.hits.clear();
        c.hits().forEach(h -> this.hits.add(WatchlistHitEntity.fromDomain(h, this)));
    }

    public KycCase toDomain() {
        RiskScore score = riskScore == null ? null : new RiskScore(riskScore, decisionReason);
        return new KycCase(id, userId, status, score, decisionReason,
                hits.stream().map(WatchlistHitEntity::toDomain).toList(),
                resolvedBy, createdAt, updatedAt);
    }
}
