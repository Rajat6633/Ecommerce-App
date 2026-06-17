package com.ecommerce.order.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Support-chat request body. The {@code question} is untrusted free text; the
 * userId is NEVER taken from the body — it comes from the JWT (owner-scoping).
 */
public record SupportChatRequest(
        @NotBlank @Size(max = 1000) String question
) {
}
