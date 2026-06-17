package com.ecommerce.order.application.port.out;

import com.ecommerce.order.domain.model.Order;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    /** Persist a new PENDING order + its initial status-history row. */
    Order createPending(Order order);

    /** Persist the (already-transitioned) order + append a status-history row. */
    Order updateStatus(Order order, String note);

    Optional<Order> findById(UUID id);

    List<Order> findByUserId(UUID userId);
}
