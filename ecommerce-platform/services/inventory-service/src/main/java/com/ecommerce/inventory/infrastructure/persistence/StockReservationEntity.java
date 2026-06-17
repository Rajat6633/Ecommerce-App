package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.domain.model.ReservationStatus;
import com.ecommerce.inventory.domain.model.StockReservation;
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
@Table(name = "stock_reservations", indexes = @Index(name = "idx_reservations_order", columnList = "order_id"))
public class StockReservationEntity {

    @Id
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ReservationStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    protected StockReservationEntity() {
    }

    public static StockReservationEntity fromDomain(StockReservation r) {
        StockReservationEntity e = new StockReservationEntity();
        e.id = r.id();
        e.orderId = r.orderId();
        e.productId = r.productId();
        e.quantity = r.quantity();
        e.status = r.status();
        e.createdAt = r.createdAt();
        return e;
    }

    public StockReservation toDomain() {
        return new StockReservation(id, orderId, productId, quantity, status, createdAt);
    }
}
