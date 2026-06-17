package com.ecommerce.kyc.domain.model;

import java.util.List;
import java.util.Objects;

/**
 * A single sanctions list record to be embedded + screened against. Parsed from
 * a feed (OFAC SDN, UN, EU, ...). Immutable, zero framework deps.
 *
 * <ul>
 *   <li>{@code source}     — list identifier, e.g. {@code "OFAC"} / {@code "UN"}.</li>
 *   <li>{@code externalId} — the feed's own id (OFAC {@code ent_num}); used as the
 *       stable upsert key so re-ingestion replaces rather than duplicates.</li>
 *   <li>{@code primaryName}— the main screening target name.</li>
 *   <li>{@code aliases}    — alternate names (a.k.a.) screened alongside the primary.</li>
 *   <li>{@code entityType} — OFAC {@code SDN_Type} (e.g. individual / entity / vessel).</li>
 * </ul>
 */
public record WatchlistEntry(
        String source,
        String externalId,
        String primaryName,
        List<String> aliases,
        String entityType
) {
    public WatchlistEntry {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(externalId, "externalId");
        Objects.requireNonNull(primaryName, "primaryName");
        aliases = aliases == null ? List.of() : List.copyOf(aliases);
    }

    /** Primary name plus aliases, de-duplicated, blanks removed — what we embed. */
    public List<String> screenableNames() {
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(primaryName), aliases.stream())
                .map(String::trim)
                .filter(n -> !n.isEmpty())
                .distinct()
                .toList();
    }
}
