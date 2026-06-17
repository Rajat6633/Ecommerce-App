package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String currency,
        List<Item> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record Item(UUID productId, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {
    }

    public static OrderResponse from(Order o) {
        List<Item> items = o.items().stream()
                .map(i -> new Item(i.productId(), i.quantity(), i.unitPrice(), i.lineTotal()))
                .toList();
        return new OrderResponse(o.id(), o.userId(), o.status(), o.totalAmount(), o.currency(),
                items, o.createdAt(), o.updatedAt());
    }
}
