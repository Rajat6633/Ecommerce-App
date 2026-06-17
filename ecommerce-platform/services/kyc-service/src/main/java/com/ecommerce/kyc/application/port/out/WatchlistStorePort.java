package com.ecommerce.kyc.application.port.out;

import com.ecommerce.kyc.domain.model.WatchlistEntry;

import java.util.Collection;

/**
 * Outbound port for persisting ingested watchlist entries into the screening
 * store (the embedding/vector store that {@link ScreeningPort} searches).
 *
 * <p>Upsert semantics: re-ingesting the same {@link WatchlistEntry#externalId()}
 * replaces the prior copy rather than duplicating it, so the scheduled refresh
 * is idempotent. Keeps Spring AI {@code Document}/{@code VectorStore} types out
 * of the application layer.
 */
public interface WatchlistStorePort {

    /** Upsert (insert-or-replace) the given entries by their external id. */
    void upsert(Collection<WatchlistEntry> entries);
}
