package com.ecommerce.kyc.domain.model;

import java.util.UUID;

/**
 * A single sanctions/watchlist match for a case. {@code score} is the embedding
 * similarity in [0.0, 1.0]; {@code payload} carries the raw matched record for
 * the officer to inspect. Immutable — zero framework deps.
 */
public record WatchlistHit(
        UUID id,
        String listSource,
        String matchedName,
        double score,
        String payload
) {
    public static WatchlistHit of(String listSource, String matchedName, double score, String payload) {
        return new WatchlistHit(UUID.randomUUID(), listSource, matchedName, score, payload);
    }
}
