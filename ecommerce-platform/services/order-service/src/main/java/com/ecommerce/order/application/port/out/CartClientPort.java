package com.ecommerce.order.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Outbound port to cart-service (resilient). */
public interface CartClientPort {

    CartSnapshot getCart(UUID userId);

    void clear(UUID userId);

    record CartSnapshot(List<Line> items) {
        public boolean isEmpty() {
            return items == null || items.isEmpty();
        }
    }

    record Line(UUID productId, int quantity, BigDecimal unitPrice) {
    }
}
