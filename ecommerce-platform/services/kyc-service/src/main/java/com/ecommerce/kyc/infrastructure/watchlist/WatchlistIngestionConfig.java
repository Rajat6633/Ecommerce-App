package com.ecommerce.kyc.infrastructure.watchlist;

import com.ecommerce.kyc.application.port.out.WatchlistFeedPort;
import com.ecommerce.kyc.application.service.WatchlistIngestionProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestClient;

import java.net.HttpURLConnection;
import java.time.Duration;

/**
 * Wires sanctions watchlist ingestion. Everything here is gated by
 * {@code kyc.watchlist.ingestion.enabled} (default <strong>false</strong>): when
 * off, no feed bean and no scheduler exist, so local/test/offline runs fall back
 * to the dev-fixture {@code WatchlistSeeder} and never touch the network.
 *
 * <p>When on, an {@link OfacSdnFeedAdapter} is contributed as a
 * {@link WatchlistFeedPort} (UN/EU adapters could be added the same way) and the
 * {@link WatchlistRefreshScheduler} drives the configurable cron refresh.
 *
 * <p>{@code @EnableConfigurationProperties} is unconditional so the properties
 * bean exists for the {@code enabled} check itself; the network-touching beans
 * are conditional.
 */
@Configuration
@EnableConfigurationProperties(WatchlistIngestionProperties.class)
public class WatchlistIngestionConfig {

    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(prefix = "kyc.watchlist.ingestion", name = "enabled", havingValue = "true")
    static class EnabledIngestion {

        @Bean
        UrlSafetyValidator watchlistUrlSafetyValidator(WatchlistIngestionProperties properties) {
            UrlSafetyValidator validator = new UrlSafetyValidator(properties.allowedHosts());
            // Fail fast at startup if ingestion is enabled but a configured URL is unsafe.
            validator.validate(properties.feedUrl());
            String altUrl = properties.altNamesUrl();
            if (altUrl != null && !altUrl.isBlank()) {
                validator.validate(altUrl);
            }
            return validator;
        }

        @Bean
        RestClient watchlistRestClient(RestClient.Builder builder) {
            // Generous timeouts: OFAC SDN is a multi-MB file; this runs off-thread on a cron.
            var factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                        throws java.io.IOException {
                    super.prepareConnection(connection, httpMethod);
                    // Disable redirect following so a public->internal 302 can't bypass
                    // the SSRF check (the validator only sees the original URL).
                    connection.setInstanceFollowRedirects(false);
                }
            };
            factory.setConnectTimeout((int) Duration.ofSeconds(15).toMillis());
            factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
            return builder.requestFactory(factory).build();
        }

        @Bean
        OfacSdnCsvParser ofacSdnCsvParser() {
            return new OfacSdnCsvParser();
        }

        @Bean
        WatchlistFeedPort ofacSdnFeedAdapter(RestClient watchlistRestClient,
                                             OfacSdnCsvParser ofacSdnCsvParser,
                                             WatchlistIngestionProperties properties,
                                             UrlSafetyValidator watchlistUrlSafetyValidator) {
            return new OfacSdnFeedAdapter(watchlistRestClient, ofacSdnCsvParser, properties,
                    watchlistUrlSafetyValidator);
        }
    }
}
