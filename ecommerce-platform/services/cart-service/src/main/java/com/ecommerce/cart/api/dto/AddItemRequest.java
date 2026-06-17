package com.ecommerce.cart.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.UUID;

public record AddItemRequest(
        @NotNull UUID productId,
        @Positive int quantity
) {
}
