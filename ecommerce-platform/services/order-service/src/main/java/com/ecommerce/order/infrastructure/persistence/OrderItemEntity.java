package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity order;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    protected OrderItemEntity() {
    }

    static OrderItemEntity of(OrderEntity order, OrderItem item) {
        OrderItemEntity e = new OrderItemEntity();
        e.id = UUID.randomUUID();
        e.order = order;
        e.productId = item.productId();
        e.quantity = item.quantity();
        e.unitPrice = item.unitPrice();
        return e;
    }

    OrderItem toDomain() {
        return new OrderItem(productId, quantity, unitPrice);
    }
}
