package com.ecommerce.kyc.infrastructure.persistence;

import com.ecommerce.kyc.domain.model.WatchlistHit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "watchlist_hits")
public class WatchlistHitEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private KycCaseEntity kycCase;

    @Column(name = "list_source", nullable = false, length = 64)
    private String listSource;

    @Column(name = "matched_name", nullable = false, length = 512)
    private String matchedName;

    @Column(nullable = false)
    private double score;

    @Column(columnDefinition = "TEXT")
    private String payload;

    protected WatchlistHitEntity() {
    }

    public static WatchlistHitEntity fromDomain(WatchlistHit h, KycCaseEntity parent) {
        WatchlistHitEntity e = new WatchlistHitEntity();
        e.id = h.id();
        e.kycCase = parent;
        e.listSource = h.listSource();
        e.matchedName = h.matchedName();
        e.score = h.score();
        e.payload = h.payload();
        return e;
    }

    public WatchlistHit toDomain() {
        return new WatchlistHit(id, listSource, matchedName, score, payload);
    }
}
