package com.ecommerce.order.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Subset of cart-service's CartResponse needed to snapshot an order. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartDto(List<Item> items) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}
