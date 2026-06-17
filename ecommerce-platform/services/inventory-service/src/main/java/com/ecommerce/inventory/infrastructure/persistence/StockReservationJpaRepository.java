package com.ecommerce.inventory.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, UUID> {

    boolean existsByOrderId(UUID orderId);

    List<StockReservationEntity> findByOrderId(UUID orderId);
}
