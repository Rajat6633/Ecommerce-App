package com.ecommerce.order.infrastructure.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.ChatOptionsBuilder;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * Deterministic offline {@link ChatModel} used under the {@code test} profile so
 * {@code mvn test} needs no Ollama and no ANTHROPIC_API_KEY. Mirrors kyc-service's
 * StubChatModel.
 *
 * <p>It echoes a short canned support reply. The content is intentionally boring —
 * tests assert on the adapter/service wiring and the prompt content, not on model
 * quality.
 */
public class StubChatModel implements ChatModel {

    @Override
    public ChatResponse call(Prompt prompt) {
        return new ChatResponse(List.of(new Generation(
                "Stubbed order-support reply (offline stub).")));
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return ChatOptionsBuilder.builder().build();
    }
}
