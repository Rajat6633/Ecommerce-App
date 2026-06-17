package com.ecommerce.kyc.infrastructure.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Deterministic offline {@link ChatModel} used under the {@code local}/{@code test}
 * profiles so {@code mvn test} needs no ANTHROPIC_API_KEY. Mirrors how
 * notification-service keeps tests SMTP-free via LoggingNotificationSender.
 *
 * <p>For an extraction prompt it returns a fixed JSON document; otherwise it
 * echoes a short canned narrative. The content is intentionally boring — tests
 * assert on the adapter mapping, not on model quality.
 */
public class StubChatModel implements ChatModel {

    private static final String STUB_DOCUMENT_JSON =
            "{\"fullName\":\"Test User\",\"documentNumber\":\"X1234567\",\"dateOfBirth\":\"1990-01-01\","
                    + "\"expiry\":\"2030-01-01\",\"nationality\":\"GB\"}";

    @Override
    public ChatResponse call(Prompt prompt) {
        String text = prompt.getContents().toLowerCase();
        String reply = text.contains("document") || text.contains("json object")
                ? STUB_DOCUMENT_JSON
                : "Stubbed risk narrative: no automated assessment performed (offline stub).";
        return new ChatResponse(List.of(new Generation(reply)));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptionsBuilder.builder().build();
    }
}
