package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.application.service.WatchlistIngestionProperties;
import com.ecommerce.kyc.domain.model.WatchlistEntry;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Offline feed-adapter test: mocked HTTP (MockRestServiceServer), real parser. */
class OfacSdnFeedAdapterTest {

    private static final String SDN_URL = "http://feed.test/sdn.csv";
    private static final String ALT_URL = "http://feed.test/alt.csv";

    /** No-op validator: these tests use mocked HTTP and a non-resolving test host. */
    private static final UrlSafetyValidator NO_OP_VALIDATOR = new UrlSafetyValidator(List.of()) {
        @Override
        public void validate(String url) {
            // SSRF validation is covered separately by UrlSafetyValidatorTest.
        }
    };

    private static String body(String path) throws IOException {
        return new ClassPathResource(path).getContentAsString(StandardCharsets.UTF_8);
    }

    private WatchlistIngestionProperties props(String altUrl) {
        return new WatchlistIngestionProperties(true, SDN_URL, altUrl, "0 0 3 * * *", 0, 0L, List.of());
    }

    private OfacSdnFeedAdapter adapter(RestClient.Builder builder, String altUrl) {
        return new OfacSdnFeedAdapter(builder.build(), new OfacSdnCsvParser(), props(altUrl), NO_OP_VALIDATOR);
    }

    @Test
    void fetchesAndParsesOverHttp_withAliases() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SDN_URL))
                .andRespond(withSuccess(body("watchlist/ofac-sdn-sample.csv"), MediaType.TEXT_PLAIN));
        server.expect(requestTo(ALT_URL))
                .andRespond(withSuccess(body("watchlist/ofac-alt-sample.csv"), MediaType.TEXT_PLAIN));

        OfacSdnFeedAdapter adapter = adapter(builder, ALT_URL);

        List<WatchlistEntry> entries = adapter.fetch();

        assertThat(adapter.source()).isEqualTo("OFAC");
        assertThat(entries).hasSize(4); // vessel filtered
        assertThat(entries).anySatisfy(e -> {
            assertThat(e.externalId()).isEqualTo("173");
            assertThat(e.aliases()).contains("ABU ABDUL AZIZ");
        });
        server.verify();
    }

    @Test
    void altFeedFailure_isToleratedAndPrimaryNamesStillIngested() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SDN_URL))
                .andRespond(withSuccess(body("watchlist/ofac-sdn-sample.csv"), MediaType.TEXT_PLAIN));
        server.expect(requestTo(ALT_URL)).andRespond(withServerError());

        OfacSdnFeedAdapter adapter = adapter(builder, ALT_URL);

        List<WatchlistEntry> entries = adapter.fetch();

        assertThat(entries).hasSize(4);
        // Aliases absent because alt feed failed, but primary names are intact.
        assertThat(entries).allSatisfy(e -> assertThat(e.aliases()).isEmpty());
    }

    @Test
    void primaryFeedFailure_throws_soUseCaseCanSkip() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(SDN_URL)).andRespond(withServerError());

        OfacSdnFeedAdapter adapter = adapter(builder, "");

        assertThatThrownBy(adapter::fetch).isInstanceOf(RuntimeException.class);
    }

    @Test
    void unsafeUrl_rejectedBeforeAnyHttpCall() {
        RestClient.Builder builder = RestClient.builder();
        // Server with NO expectations: any HTTP attempt would fail verification.
        MockRestServiceServer.bindTo(builder).build();

        UrlSafetyValidator rejecting = new UrlSafetyValidator(List.of()) {
            @Override
            public void validate(String url) {
                throw new UnsafeUrlException("blocked: " + url);
            }
        };
        OfacSdnFeedAdapter adapter =
                new OfacSdnFeedAdapter(builder.build(), new OfacSdnCsvParser(), props(""), rejecting);

        assertThatThrownBy(adapter::fetch)
                .isInstanceOf(UrlSafetyValidator.UnsafeUrlException.class);
    }

    @Test
    void responseOverByteCap_throws() throws IOException {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        // 2KB body, 1KB cap -> must abort.
        String big = "x".repeat(2048);
        server.expect(requestTo(SDN_URL)).andRespond(withSuccess(big, MediaType.TEXT_PLAIN));

        WatchlistIngestionProperties capped =
                new WatchlistIngestionProperties(true, SDN_URL, "", "0 0 3 * * *", 0, 1024L, List.of());
        OfacSdnFeedAdapter adapter =
                new OfacSdnFeedAdapter(builder.build(), new OfacSdnCsvParser(), capped, NO_OP_VALIDATOR);

        assertThatThrownBy(adapter::fetch).isInstanceOf(RuntimeException.class);
    }
}
