package com.ecommerce.inventory.api.dto;

import jakarta.validation.constraints.Positive;

public record ReceiveStockRequest(
        @Positive int quantity
) {
}
