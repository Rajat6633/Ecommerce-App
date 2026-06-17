package com.ecommerce.cart.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/** Subset of product-service's ProductResponse needed by the cart. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductDto(
        UUID id,
        BigDecimal price,
        String currency,
        boolean active
) {
}
