package com.ecommerce.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CustomerKycStatusJpaRepository extends JpaRepository<CustomerKycStatusEntity, UUID> {
}
