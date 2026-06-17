package com.ecommerce.order.infrastructure.ai;

import com.ecommerce.order.infrastructure.config.StubChatModel;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatAssistantAdapterTest {

    private final ChatAssistantAdapter adapter =
            new ChatAssistantAdapter(new StubChatModel(), new SimpleMeterRegistry());

    @Test
    void ask_returnsModelReplyViaStub() {
        String reply = adapter.ask("You are an order-support assistant.",
                "Order context: ... Question: where is my order?");

        assertThat(reply).isNotBlank().contains("offline stub");
    }

    @Test
    void failSoft_outageYieldsFriendlyFallback() {
        String reply = adapter.askFallback("system", "user", new RuntimeException("model down"));

        assertThat(reply)
                .contains("temporarily unavailable")
                .doesNotContain("Exception");
    }
}
