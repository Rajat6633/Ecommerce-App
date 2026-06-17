package com.ecommerce.kyc.api.dto;

import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record KycCaseResponse(
        UUID id,
        UUID userId,
        KycStatus status,
        Double riskScore,
        String decisionReason,
        String narrative,
        List<WatchlistHitResponse> hits,
        String resolvedBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static KycCaseResponse from(KycCase c) {
        return new KycCaseResponse(
                c.id(), c.userId(), c.status(),
                c.riskScore() == null ? null : c.riskScore().value(),
                c.decisionReason(),
                c.riskScore() == null ? null : c.riskScore().narrative(),
                c.hits().stream().map(WatchlistHitResponse::from).toList(),
                c.resolvedBy(), c.createdAt(), c.updatedAt());
    }
}
