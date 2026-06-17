package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.domain.model.WatchlistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hand-rolled parser for the OFAC SDN flat files (sdn.csv + alt.csv). These are
 * headerless, comma-separated, double-quote-quoted files with a fixed column
 * order documented by OFAC. No CSV library is on this module's classpath and the
 * format is simple + fixed, so we parse it directly (handling quoted fields,
 * embedded commas, and doubled-quote escaping) rather than adding a dependency.
 *
 * <p><strong>sdn.csv</strong> columns (1-indexed, the ones we use):
 * <pre>
 *   [0] ent_num    — entity number (stable id; our externalId)
 *   [1] SDN_Name   — primary name (screening target)
 *   [2] SDN_Type   — "individual" / "entity" / "vessel" / "aircraft" / "-0-"
 *   [3] Program    — sanctions program(s)
 *   ... title, call_sign, vess_type, tonnage, GRT, vess_flag, vess_owner, remarks
 * </pre>
 * OFAC encodes "no value" as the literal {@code -0-}; we treat that as empty.
 *
 * <p><strong>alt.csv</strong> columns:
 * <pre>
 *   [0] ent_num    — foreign key back to sdn.csv ent_num
 *   [1] alt_num    — alias row id (unused)
 *   [2] alt_type   — "aka" / "fka" / "nka"
 *   [3] alt_name   — the alias (a.k.a.) name
 * </pre>
 * Aliases are grouped by ent_num and attached to the matching SDN entry, so each
 * individual/entity is embedded under its primary name plus all known aliases.
 */
public class OfacSdnCsvParser {

    private static final Logger log = LoggerFactory.getLogger(OfacSdnCsvParser.class);

    public static final String SOURCE = "OFAC";
    private static final String NO_VALUE = "-0-";

    /**
     * Parse the SDN list, attaching aliases from the (optional) alt file.
     *
     * @param sdnReader headerless sdn.csv reader (required)
     * @param altReader headerless alt.csv reader (may be {@code null} if not loaded)
     * @param sdnTypes  SDN_Type values to keep (lower-cased), e.g. {individual, entity}; empty = keep all
     * @param maxEntries cap on entries returned (0 = unlimited)
     */
    public List<WatchlistEntry> parse(Reader sdnReader, Reader altReader, Set<String> sdnTypes, int maxEntries) {
        Map<String, List<String>> aliasesByEnt = altReader == null ? Map.of() : parseAliases(altReader);

        List<WatchlistEntry> entries = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(sdnReader)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = splitCsv(line);
                if (cols.size() < 3) {
                    continue;
                }
                String entNum = clean(cols.get(0));
                String name = clean(cols.get(1));
                String type = clean(cols.get(2));
                if (entNum.isEmpty() || name.isEmpty()) {
                    continue;
                }
                if (!sdnTypes.isEmpty() && (type == null || !sdnTypes.contains(type.toLowerCase()))) {
                    continue;
                }
                entries.add(new WatchlistEntry(SOURCE, entNum, name,
                        aliasesByEnt.getOrDefault(entNum, List.of()), type));
                if (maxEntries > 0 && entries.size() >= maxEntries) {
                    log.info("OFAC SDN parse hit maxEntries cap of {}", maxEntries);
                    break;
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading OFAC SDN feed", e);
        }
        log.info("Parsed {} OFAC SDN entries (aliases for {} entities)", entries.size(), aliasesByEnt.size());
        return entries;
    }

    private Map<String, List<String>> parseAliases(Reader altReader) {
        Map<String, List<String>> byEnt = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(altReader)) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                List<String> cols = splitCsv(line);
                if (cols.size() < 4) {
                    continue;
                }
                String entNum = clean(cols.get(0));
                String altName = clean(cols.get(3));
                if (entNum.isEmpty() || altName.isEmpty()) {
                    continue;
                }
                byEnt.computeIfAbsent(entNum, k -> new ArrayList<>()).add(altName);
            }
        } catch (IOException e) {
            // Aliases are best-effort enrichment — never fail the whole ingest over them.
            log.warn("Failed reading OFAC alt-names feed; continuing without aliases ({})", e.toString());
            return Map.of();
        }
        return byEnt;
    }

    /** OFAC uses "-0-" for null; collapse internal whitespace and trim. */
    private static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String v = raw.trim();
        if (v.equals(NO_VALUE)) {
            return "";
        }
        return v.replaceAll("\\s+", " ");
    }

    /**
     * Split a single CSV record honoring double-quote quoting and doubled-quote
     * ("") escaping. OFAC fields are wrapped in quotes and may contain commas.
     */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    out.add(field.toString());
                    field.setLength(0);
                } else {
                    field.append(c);
                }
            }
        }
        out.add(field.toString());
        return out;
    }
}
