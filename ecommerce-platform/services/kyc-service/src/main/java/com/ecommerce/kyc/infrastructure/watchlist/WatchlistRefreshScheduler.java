package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.application.port.in.IngestWatchlistUseCase;
import com.ecommerce.kyc.application.port.in.IngestWatchlistUseCase.IngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Drives the sanctions watchlist refresh on the configured cron
 * ({@code kyc.watchlist.ingestion.refresh-cron}, daily by default) plus once
 * shortly after startup so a freshly-deployed pod ingests without waiting for
 * the next cron tick.
 *
 * <p>Only present when {@code kyc.watchlist.ingestion.enabled=true}. The
 * use-case is fail-closed (it swallows per-feed failures), and we also guard the
 * scheduler body so a throw can never kill the scheduling thread or startup.
 */
@Component
@ConditionalOnProperty(prefix = "kyc.watchlist.ingestion", name = "enabled", havingValue = "true")
public class WatchlistRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(WatchlistRefreshScheduler.class);

    private final IngestWatchlistUseCase ingestWatchlist;

    public WatchlistRefreshScheduler(IngestWatchlistUseCase ingestWatchlist) {
        this.ingestWatchlist = ingestWatchlist;
    }

    /** Initial load after the context is ready (off the startup critical path). */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        log.info("Watchlist ingestion enabled — running initial refresh");
        runQuietly();
    }

    /** Scheduled refresh on the configurable cron. */
    @Scheduled(cron = "${kyc.watchlist.ingestion.refresh-cron:0 0 3 * * *}")
    public void scheduledRefresh() {
        log.info("Running scheduled watchlist refresh");
        runQuietly();
    }

    private void runQuietly() {
        try {
            IngestionResult result = ingestWatchlist.ingest();
            log.info("Watchlist refresh complete: {} entries upserted, {} feeds failed",
                    result.entriesUpserted(), result.feedsFailed());
        } catch (RuntimeException e) {
            // Belt-and-suspenders: the use-case already isolates feed failures.
            log.error("Watchlist refresh aborted unexpectedly — existing data retained", e);
        }
    }
}
