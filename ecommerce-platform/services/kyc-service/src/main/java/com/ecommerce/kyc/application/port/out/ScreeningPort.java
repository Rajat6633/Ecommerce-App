package com.ecommerce.kyc.application.port.out;

import com.ecommerce.kyc.domain.model.WatchlistHit;

import java.util.List;

/**
 * Sanctions / watchlist name screening. Implemented over an embedding model +
 * pgvector VectorStore. Returns the hits above the configured similarity
 * threshold (empty list = clean). Implementations must fail closed — see the
 * adapter, which returns a synthetic hit on AI outage so the case is reviewed.
 */
public interface ScreeningPort {

    /** @return hits above threshold; empty list means a clean screen. */
    List<WatchlistHit> screen(String fullName);
}
