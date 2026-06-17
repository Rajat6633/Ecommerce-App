package com.ecommerce.kyc.application.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Tunables for scheduled sanctions watchlist ingestion (prefix
 * {@code kyc.watchlist.ingestion}).
 *
 * <ul>
 *   <li>{@code enabled}     — master switch. Default <strong>false</strong> so
 *       local/test and no-network environments keep using the fixture
 *       {@code WatchlistSeeder}; ops turns it on where the feed is reachable.</li>
 *   <li>{@code feedUrl}     — OFAC SDN primary feed URL (CSV).</li>
 *   <li>{@code altNamesUrl} — OFAC alternate-names (alias) feed URL (CSV); blank disables alias loading.</li>
 *   <li>{@code refreshCron} — cron for the scheduled refresh (default daily 03:00).</li>
 *   <li>{@code maxEntries}  — safety cap on how many SDN rows to ingest per run (default 50000; 0 = unlimited).</li>
 *   <li>{@code maxResponseBytes} — hard cap on bytes read from a feed response
 *       (default 50MB); guards against OOM / decompression bombs.</li>
 *   <li>{@code allowedHosts} — SSRF allow-list of permitted feed hosts; when
 *       empty, any publicly-resolving https host is permitted (still subject to
 *       the private/loopback/link-local rejection rules).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "kyc.watchlist.ingestion")
public record WatchlistIngestionProperties(
        boolean enabled,
        String feedUrl,
        String altNamesUrl,
        String refreshCron,
        int maxEntries,
        long maxResponseBytes,
        List<String> allowedHosts
) {
    public WatchlistIngestionProperties {
        if (feedUrl == null || feedUrl.isBlank()) {
            feedUrl = "https://www.treasury.gov/ofac/downloads/sdn.csv";
        }
        if (altNamesUrl == null) {
            altNamesUrl = "https://www.treasury.gov/ofac/downloads/alt.csv";
        }
        if (refreshCron == null || refreshCron.isBlank()) {
            refreshCron = "0 0 3 * * *";
        }
        if (maxEntries < 0) {
            maxEntries = 0;
        } else if (maxEntries == 0) {
            // A sane non-zero default; 0 only takes effect when explicitly configured.
            maxEntries = 50_000;
        }
        if (maxResponseBytes <= 0) {
            maxResponseBytes = 52_428_800L; // 50 MB
        }
        if (allowedHosts == null || allowedHosts.isEmpty()) {
            allowedHosts = List.of("www.treasury.gov");
        }
    }
}
