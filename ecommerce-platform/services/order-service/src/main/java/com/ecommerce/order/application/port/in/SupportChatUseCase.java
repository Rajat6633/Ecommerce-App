package com.ecommerce.order.application.port.in;

import java.util.UUID;

/**
 * Support chatbot (roadmap D4). Answers a natural-language question about the
 * authenticated user's OWN orders, grounded in their local order history
 * (RAG over {@code order_db} — no cross-service call).
 */
public interface SupportChatUseCase {

    /**
     * Answer {@code question} using only {@code userId}'s order history as context.
     *
     * @param userId   the authenticated principal — the ONLY user whose orders are
     *                 ever read (owner-scoped; callers must pass the JWT subject,
     *                 never a body-supplied id)
     * @param question the user's natural-language question (untrusted input)
     * @return a friendly answer; on model outage a fail-soft "support is temporarily
     *         unavailable" message — never an exception
     */
    String answer(UUID userId, String question);
}
