package com.ecommerce.kyc.application.service;

import com.ecommerce.kyc.application.port.in.IngestWatchlistUseCase;
import com.ecommerce.kyc.application.port.out.WatchlistFeedPort;
import com.ecommerce.kyc.application.port.out.WatchlistStorePort;
import com.ecommerce.kyc.domain.model.WatchlistEntry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Sanctions watchlist ingestion use-case. Iterates every configured
 * {@link WatchlistFeedPort}, fetches + parses it, and upserts the parsed entries
 * into the screening store via {@link WatchlistStorePort}.
 *
 * <p><strong>Fail-closed / fault-isolated:</strong> each feed is fetched inside
 * its own try/catch. A failing feed (network down, parse error, store error) is
 * logged and skipped — it never propagates, so it cannot crash the scheduler or
 * startup and the previously-ingested data stays intact. This mirrors the
 * module's offline-test, fail-closed ethos.
 *
 * <p>Pure application code: no HTTP, no Spring AI types — those live in the
 * infrastructure adapters behind the ports.
 */
@Service
public class WatchlistIngestionService implements IngestWatchlistUseCase {

    private static final Logger log = LoggerFactory.getLogger(WatchlistIngestionService.class);

    private final List<WatchlistFeedPort> feeds;
    private final WatchlistStorePort store;
    private final MeterRegistry meterRegistry;

    public WatchlistIngestionService(List<WatchlistFeedPort> feeds,
                                     WatchlistStorePort store,
                                     MeterRegistry meterRegistry) {
        this.feeds = feeds;
        this.store = store;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public IngestionResult ingest() {
        int totalUpserted = 0;
        int feedsFailed = 0;

        for (WatchlistFeedPort feed : feeds) {
            String source = feed.source();
            try {
                List<WatchlistEntry> entries = feed.fetch();
                store.upsert(entries);
                totalUpserted += entries.size();
                meterRegistry.counter("kyc_watchlist_ingested_total", "source", source)
                        .increment(entries.size());
                log.info("Watchlist ingestion: upserted {} entries from {}", entries.size(), source);
            } catch (RuntimeException e) {
                // Fail-closed: log + skip this feed; existing screening data is untouched.
                feedsFailed++;
                meterRegistry.counter("kyc_watchlist_ingestion_failures_total", "source", source).increment();
                log.warn("Watchlist ingestion from {} failed — keeping existing data ({})",
                        source, e.toString());
            }
        }

        if (feeds.isEmpty()) {
            log.info("Watchlist ingestion: no feeds configured — nothing to do");
        }
        return new IngestionResult(totalUpserted, feedsFailed);
    }
}
