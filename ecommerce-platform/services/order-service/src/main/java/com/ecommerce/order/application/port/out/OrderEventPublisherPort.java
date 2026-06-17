package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.Order;

import java.util.UUID;

public interface OrderEventPublisherPort {

    void publishOrderCreated(Order order);

    void publishOrderConfirmed(UUID orderId, UUID userId);
}
