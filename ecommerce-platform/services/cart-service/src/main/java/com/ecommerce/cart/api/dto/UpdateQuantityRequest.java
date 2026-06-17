package com.ecommerce.cart.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpdateQuantityRequest(
        @PositiveOrZero int quantity   // 0 removes the line
) {
}
