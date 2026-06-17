package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItemEntity> items = new ArrayList<>();

    @Version
    private Long version;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected OrderEntity() {
    }

    public static OrderEntity fromDomain(Order o) {
        OrderEntity e = new OrderEntity();
        e.id = o.id();
        e.userId = o.userId();
        e.status = o.status();
        e.totalAmount = o.totalAmount();
        e.currency = o.currency();
        e.version = o.version();
        e.createdAt = o.createdAt();
        e.updatedAt = o.updatedAt();
        e.items = o.items().stream().map(i -> OrderItemEntity.of(e, i))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        return e;
    }

    public Order toDomain() {
        return new Order(id, userId, status, totalAmount, currency,
                items.stream().map(OrderItemEntity::toDomain).toList(),
                version, createdAt, updatedAt);
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
