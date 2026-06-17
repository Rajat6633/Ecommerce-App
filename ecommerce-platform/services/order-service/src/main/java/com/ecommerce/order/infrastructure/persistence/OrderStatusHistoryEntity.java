package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_history",
        indexes = @Index(name = "idx_history_order", columnList = "order_id"))
public class OrderStatusHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(length = 255)
    private String note;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    protected OrderStatusHistoryEntity() {
    }

    public OrderStatusHistoryEntity(UUID id, UUID orderId, OrderStatus status, String note, Instant changedAt) {
        this.id = id;
        this.orderId = orderId;
        this.status = status;
        this.note = note;
        this.changedAt = changedAt;
    }
}
