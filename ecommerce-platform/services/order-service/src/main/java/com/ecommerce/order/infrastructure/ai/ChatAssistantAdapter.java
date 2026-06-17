package com.ecommerce.order.infrastructure.ai;

import com.ecommerce.order.application.port.out.ChatAssistantPort;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;

/**
 * Calls the LLM chat model (Spring AI {@link ChatClient}) for the support
 * chatbot (D4). Resilience4j-wrapped and FAIL-SOFT: on model outage / back-
 * pressure it returns a friendly "support temporarily unavailable" string via
 * the fallback method, so a model problem never surfaces as a 500. Mirrors the
 * kyc-service {@code RiskNarrativeAdapter} wiring.
 */
@Component
public class ChatAssistantAdapter implements ChatAssistantPort {

    private static final Logger log = LoggerFactory.getLogger(ChatAssistantAdapter.class);
    private static final String CB = "order-support-ai";

    private static final String FALLBACK =
            "Sorry, order support is temporarily unavailable. Please try again in a few minutes.";

    private final ChatClient chatClient;
    private final Timer latency;

    public ChatAssistantAdapter(ChatModel chatModel, MeterRegistry meterRegistry) {
        this.chatClient = ChatClient.create(chatModel);
        this.latency = Timer.builder("order_support_ai_call_latency")
                .tag("op", "chat").register(meterRegistry);
        meterRegistry.counter("order_support_ai_calls_total", "op", "chat");
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "askFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public String ask(String systemPrompt, String userPrompt) {
        return latency.record(() ->
                chatClient.prompt().system(systemPrompt).user(userPrompt).call().content());
    }

    @SuppressWarnings("unused")
    String askFallback(String systemPrompt, String userPrompt, Throwable t) {
        log.warn("Support chat unavailable ({}) — returning fail-soft message", t.toString());
        return FALLBACK;
    }
}
