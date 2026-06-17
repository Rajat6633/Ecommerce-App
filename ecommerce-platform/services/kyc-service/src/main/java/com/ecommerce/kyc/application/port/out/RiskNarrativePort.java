package com.ecommerce.kyc.application.port.out;

import com.ecommerce.kyc.domain.model.WatchlistHit;

import java.util.List;

/**
 * Turns structured risk signals into a human-readable narrative for a compliance
 * officer (Claude chat). Must fail closed — a deterministic fallback string on
 * outage, never an exception that would auto-approve.
 */
public interface RiskNarrativePort {

    String summarise(String fullName, List<WatchlistHit> hits);
}
