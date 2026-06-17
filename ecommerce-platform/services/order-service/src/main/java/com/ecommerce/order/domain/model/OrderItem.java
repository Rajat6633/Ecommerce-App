package com.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(UUID productId, int quantity, BigDecimal unitPrice) {
    public OrderItem {
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be > 0");
    }

    public BigDecimal lineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
