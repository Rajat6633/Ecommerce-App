package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.domain.model.WatchlistHit;
import com.ecommerce.kyc.infrastructure.config.StubChatModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RiskNarrativeAdapterTest {

    private final RiskNarrativeAdapter adapter =
            new RiskNarrativeAdapter(new StubChatModel(), new SimpleMeterRegistry());

    @Test
    void noHits_returnsCleanNarrativeWithoutCallingModel() {
        String narrative = adapter.summarise("Jane Doe", List.of());

        assertThat(narrative).contains("No sanctions").contains("Jane Doe");
    }

    @Test
    void withHits_summarisesViaStubModel() {
        String narrative = adapter.summarise("Viktor Petrov",
                List.of(WatchlistHit.of("OFAC", "Viktor Petrov", 0.93, "raw")));

        assertThat(narrative).isNotBlank();
    }

    @Test
    void failClosed_outageYieldsDeterministicFallback() {
        String narrative = adapter.summariseFallback("Jane Doe",
                List.of(WatchlistHit.of("OFAC", "Jane Doe", 0.9, "raw")), new RuntimeException("down"));

        assertThat(narrative).contains("manual review").contains("Jane Doe");
    }
}
