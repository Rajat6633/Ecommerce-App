package com.ecommerce.kyc.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Officer decision body for POST /api/kyc/{userId}/resolve. */
public record ResolveCaseRequest(
        @NotNull Boolean approve,
        @Size(max = 1024) String reason
) {
}
