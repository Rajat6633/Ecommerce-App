package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.domain.model.WatchlistEntry;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Offline parse of the sample OFAC SDN + alt fixtures — no HTTP. */
class OfacSdnCsvParserTest {

    private final OfacSdnCsvParser parser = new OfacSdnCsvParser();

    private static Reader fixture(String path) {
        var in = OfacSdnCsvParserTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("Missing test fixture: " + path);
        }
        return new InputStreamReader(in, StandardCharsets.UTF_8);
    }

    @Test
    void parsesIndividualsAndEntities_attachesAliases_filtersVessels() {
        List<WatchlistEntry> entries = parser.parse(
                fixture("watchlist/ofac-sdn-sample.csv"),
                fixture("watchlist/ofac-alt-sample.csv"),
                Set.of("individual", "entity"),
                0);

        // 5 rows: 2 individuals + 2 entities kept, 1 vessel filtered out => 4.
        assertThat(entries).hasSize(4);
        assertThat(entries).allSatisfy(e -> assertThat(e.source()).isEqualTo("OFAC"));
        assertThat(entries).noneSatisfy(e -> assertThat(e.entityType()).isEqualTo("vessel"));

        WatchlistEntry abuAziz = entries.stream()
                .filter(e -> e.externalId().equals("173")).findFirst().orElseThrow();
        assertThat(abuAziz.primaryName()).isEqualTo("ABDUL AZIZ, Abu");
        assertThat(abuAziz.entityType()).isEqualTo("individual");
        // Two aliases attached from alt.csv for ent_num 173.
        assertThat(abuAziz.aliases()).containsExactly("ABU ABDUL AZIZ", "AZIZ, Abdul");
        // screenableNames = primary + aliases (deduped).
        assertThat(abuAziz.screenableNames())
                .containsExactly("ABDUL AZIZ, Abu", "ABU ABDUL AZIZ", "AZIZ, Abdul");
    }

    @Test
    void handlesQuotedFieldsWithEmbeddedCommas() {
        List<WatchlistEntry> entries = parser.parse(
                fixture("watchlist/ofac-sdn-sample.csv"), null, Set.of("entity"), 0);

        WatchlistEntry gulf = entries.stream()
                .filter(e -> e.externalId().equals("500")).findFirst().orElseThrow();
        // The comma inside the quoted SDN_Name must stay intact.
        assertThat(gulf.primaryName()).isEqualTo("GULF EXPORT, S.A.");
    }

    @Test
    void emptyTypeFilter_keepsEverythingExceptBlanks() {
        List<WatchlistEntry> entries = parser.parse(
                fixture("watchlist/ofac-sdn-sample.csv"), null, Set.of(), 0);
        assertThat(entries).hasSize(5); // includes the vessel when no type filter
    }

    @Test
    void maxEntriesCapsResults() {
        List<WatchlistEntry> entries = parser.parse(
                fixture("watchlist/ofac-sdn-sample.csv"), null, Set.of(), 2);
        assertThat(entries).hasSize(2);
    }

    @Test
    void splitCsv_handlesDoubledQuoteEscaping() {
        List<String> cols = OfacSdnCsvParser.splitCsv("\"a\",\"b\"\"c\",\"d,e\"");
        assertThat(cols).containsExactly("a", "b\"c", "d,e");
    }
}
