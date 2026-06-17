package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.application.port.out.WatchlistFeedPort;
import com.ecommerce.kyc.application.service.WatchlistIngestionProperties;
import com.ecommerce.kyc.domain.model.WatchlistEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/**
 * OFAC SDN {@link WatchlistFeedPort}: fetches the SDN CSV (and, when configured,
 * the alternate-names CSV) over HTTP using Spring's {@link RestClient}, then
 * delegates parsing to {@link OfacSdnCsvParser}.
 *
 * <p>HTTP + Spring AI live here in infrastructure; the parser is a pure POJO so
 * it can be unit-tested offline against a sample fixture. A fetch failure throws
 * — the {@code WatchlistIngestionService} use-case catches it per feed and skips,
 * so the feed never crashes startup or the scheduler.
 *
 * <p><strong>SSRF guard:</strong> every feed URL is validated through a
 * {@link UrlSafetyValidator} immediately before the request (https-only, public
 * unicast hosts, optional allow-list). <strong>OOM/bomb guard:</strong> the
 * response body is streamed and hard-capped at
 * {@code kyc.watchlist.ingestion.max-response-bytes}; we also request
 * {@code Accept-Encoding: identity} so a gzip bomb cannot expand unbounded.
 *
 * <p>Only screens individuals + entities (vessels/aircraft are excluded as
 * non-person screening targets).
 */
public class OfacSdnFeedAdapter implements WatchlistFeedPort {

    private static final Logger log = LoggerFactory.getLogger(OfacSdnFeedAdapter.class);
    private static final Set<String> SCREENED_TYPES = Set.of("individual", "entity");

    private final RestClient restClient;
    private final OfacSdnCsvParser parser;
    private final WatchlistIngestionProperties properties;
    private final UrlSafetyValidator urlSafetyValidator;

    public OfacSdnFeedAdapter(RestClient restClient,
                              OfacSdnCsvParser parser,
                              WatchlistIngestionProperties properties,
                              UrlSafetyValidator urlSafetyValidator) {
        this.restClient = restClient;
        this.parser = parser;
        this.properties = properties;
        this.urlSafetyValidator = urlSafetyValidator;
    }

    @Override
    public String source() {
        return OfacSdnCsvParser.SOURCE;
    }

    @Override
    public List<WatchlistEntry> fetch() {
        log.info("Fetching OFAC SDN feed from {}", properties.feedUrl());
        String sdnCsv = get(properties.feedUrl());
        if (sdnCsv == null || sdnCsv.isBlank()) {
            throw new IllegalStateException("OFAC SDN feed returned empty body");
        }

        StringReader altReader = null;
        String altUrl = properties.altNamesUrl();
        if (altUrl != null && !altUrl.isBlank()) {
            try {
                String altCsv = get(altUrl);
                if (altCsv != null && !altCsv.isBlank()) {
                    altReader = new StringReader(altCsv);
                }
            } catch (RuntimeException e) {
                // Aliases are best-effort; carry on with primary names only.
                log.warn("OFAC alt-names fetch failed; ingesting primary names only ({})", e.toString());
            }
        }

        return parser.parse(new StringReader(sdnCsv), altReader, SCREENED_TYPES, properties.maxEntries());
    }

    private String get(String url) {
        // SSRF guard: re-validate immediately before every fetch (DNS may have
        // changed since startup, and this also covers the alt-names URL).
        urlSafetyValidator.validate(url);
        long cap = properties.maxResponseBytes();
        return restClient.get()
                .uri(url)
                // Request identity so a gzip bomb cannot expand unbounded; the cap
                // below then bounds the (decompressed) bytes we will hold.
                .header(HttpHeaders.ACCEPT_ENCODING, "identity")
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw new IllegalStateException(
                                "OFAC feed returned HTTP " + response.getStatusCode().value() + ": " + url);
                    }
                    try (InputStream in = response.getBody()) {
                        return readCapped(in, cap, url);
                    }
                });
    }

    /** Reads at most {@code cap} bytes; aborts (throws) once the cap is exceeded. */
    private static String readCapped(InputStream in, long cap, String url) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(chunk)) != -1) {
            total += read;
            if (total > cap) {
                throw new IllegalStateException(
                        "OFAC feed response exceeded the " + cap + "-byte cap: " + url);
            }
            buffer.write(chunk, 0, read);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
