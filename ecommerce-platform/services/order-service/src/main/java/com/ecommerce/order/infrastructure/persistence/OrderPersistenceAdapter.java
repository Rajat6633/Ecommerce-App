package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.application.port.out.OrderRepositoryPort;
import com.ecommerce.order.domain.exception.OrderNotFoundException;
import com.ecommerce.order.domain.model.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class OrderPersistenceAdapter implements OrderRepositoryPort {

    private final OrderJpaRepository orders;
    private final OrderStatusHistoryJpaRepository history;
    private final Clock clock;

    public OrderPersistenceAdapter(OrderJpaRepository orders,
                                   OrderStatusHistoryJpaRepository history,
                                   Clock clock) {
        this.orders = orders;
        this.history = history;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Order createPending(Order order) {
        OrderEntity saved = orders.save(OrderEntity.fromDomain(order));
        appendHistory(saved.toDomain(), "order placed");
        return saved.toDomain();
    }

    @Override
    @Transactional
    public Order updateStatus(Order order, String note) {
        OrderEntity entity = orders.findById(order.id())
                .orElseThrow(() -> new OrderNotFoundException(order.id().toString()));
        entity.setStatus(order.status());
        entity.setUpdatedAt(order.updatedAt());
        OrderEntity saved = orders.save(entity);
        appendHistory(saved.toDomain(), note);
        return saved.toDomain();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(UUID id) {
        return orders.findById(id).map(OrderEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByUserId(UUID userId) {
        return orders.findByUserIdOrderByCreatedAtDesc(userId).stream().map(OrderEntity::toDomain).toList();
    }

    private void appendHistory(Order order, String note) {
        history.save(new OrderStatusHistoryEntity(
                UUID.randomUUID(), order.id(), order.status(), note, clock.instant()));
    }
}
