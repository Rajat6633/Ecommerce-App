package com.ecommerce.order.application.port.in;

import com.ecommerce.order.domain.model.Order;

import java.util.List;
import java.util.UUID;

/** Query side: order history + lookup (with ownership enforcement). */
public interface OrderQueryUseCase {

    List<Order> history(UUID userId);

    Order getForUser(UUID orderId, UUID requesterId, boolean isAdmin);
}
