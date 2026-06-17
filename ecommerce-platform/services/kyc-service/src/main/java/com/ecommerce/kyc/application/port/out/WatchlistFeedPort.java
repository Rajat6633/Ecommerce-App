package com.ecommerce.kyc.application.port.out;

import com.ecommerce.kyc.domain.model.WatchlistEntry;

import java.util.List;

/**
 * Pluggable sanctions feed. One implementation per list (OFAC SDN today; UN/EU
 * can be added later by adding another adapter). The application use-case does
 * not know whether the feed is HTTP/CSV/XML — it only asks for parsed entries.
 *
 * <p>Implementations are responsible for fetching + parsing and MUST be
 * fail-closed at the boundary: a fetch/parse failure surfaces as a thrown
 * exception that the ingestion use-case swallows per source, so one bad feed
 * never aborts the others nor crashes the scheduler. Domain/application stay
 * free of HTTP and Spring types.
 */
public interface WatchlistFeedPort {

    /** Stable identifier for this feed/list, e.g. {@code "OFAC"}. */
    String source();

    /**
     * Fetch + parse the current feed.
     *
     * @return parsed entries (may be empty); never {@code null}.
     * @throws RuntimeException on any fetch/parse failure — the caller decides
     *                          how to contain it (the ingestion use-case logs + skips).
     */
    List<WatchlistEntry> fetch();
}
