package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;
import com.ecommerce.kyc.infrastructure.config.StubChatModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentExtractionAdapterTest {

    private final DocumentExtractionAdapter adapter =
            new DocumentExtractionAdapter(new StubChatModel(), new SimpleMeterRegistry());

    @Test
    void extract_mapsStubJsonToDomain() {
        ExtractedDocument doc = adapter.extract(new byte[]{1, 2, 3}, "image/png");

        assertThat(doc.confident()).isTrue();
        assertThat(doc.fullName()).isEqualTo("Test User");
        assertThat(doc.documentNumber()).isEqualTo("X1234567");
    }

    @Test
    void failClosed_outageYieldsLowConfidence() {
        ExtractedDocument doc = adapter.extractFallback(new byte[]{1}, "image/png", new RuntimeException("down"));

        assertThat(doc.confident()).isFalse();
        assertThat(doc.fullName()).isNull();
    }
}
