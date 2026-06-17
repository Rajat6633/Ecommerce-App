package com.ecommerce.order.api.dto;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;

import java.util.UUID;

public record OrderStatusResponse(UUID orderId, OrderStatus status) {
    public static OrderStatusResponse from(Order o) {
        return new OrderStatusResponse(o.id(), o.status());
    }
}
