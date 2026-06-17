package com.ecommerce.kyc.api.dto;

import com.ecommerce.kyc.domain.model.WatchlistHit;

import java.util.UUID;

public record WatchlistHitResponse(
        UUID id,
        String listSource,
        String matchedName,
        double score,
        String payload
) {
    public static WatchlistHitResponse from(WatchlistHit h) {
        return new WatchlistHitResponse(h.id(), h.listSource(), h.matchedName(), h.score(), h.payload());
    }
}
