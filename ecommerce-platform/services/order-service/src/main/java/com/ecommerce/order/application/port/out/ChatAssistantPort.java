package com.ecommerce.order.application.port.out;

/**
 * Outbound boundary to the LLM chat model (Spring AI {@link
 * org.springframework.ai.chat.client.ChatClient} behind the adapter). Kept free
 * of Spring AI types so the application/domain layers don't depend on the
 * framework. Must fail soft — a friendly fallback string on model outage, never
 * an exception (the adapter is Resilience4j-wrapped with a fallback method).
 */
public interface ChatAssistantPort {

    /**
     * @param systemPrompt grounding instructions (role + injection guard)
     * @param userPrompt   the order context + the user's delimited question
     * @return the model's answer, or a fail-soft fallback on outage
     */
    String ask(String systemPrompt, String userPrompt);
}
