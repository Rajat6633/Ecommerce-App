package com.ecommerce.kyc.application.port.in;

/**
 * Inbound use-case: refresh the sanctions watchlist from its configured feeds.
 * Triggered on a schedule (and could be triggered on demand). Implementations
 * MUST be fail-closed — a feed outage logs and leaves the existing screening
 * data intact rather than throwing.
 */
public interface IngestWatchlistUseCase {

    /**
     * Run ingestion across all configured feeds.
     *
     * @return a summary of how many entries were upserted per source.
     */
    IngestionResult ingest();

    /** Per-run summary: total entries upserted and how many feeds failed. */
    record IngestionResult(int entriesUpserted, int feedsFailed) {
    }
}
