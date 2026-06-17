package com.ecommerce.inventory.api.dto;

import jakarta.validation.constraints.PositiveOrZero;

public record UpsertStockRequest(
        @PositiveOrZero int onHand,
        @PositiveOrZero int reorderLevel
) {
}
