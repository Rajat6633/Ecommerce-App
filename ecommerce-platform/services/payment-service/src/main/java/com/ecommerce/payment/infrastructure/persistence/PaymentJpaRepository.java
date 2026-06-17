package com.ecommerce.payment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentEntity, UUID> {

    boolean existsByOrderId(UUID orderId);

    Optional<PaymentEntity> findByOrderId(UUID orderId);
}
