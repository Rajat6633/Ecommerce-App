package com.ecommerce.order.infrastructure.persistence;

import com.ecommerce.order.domain.model.CustomerKycStatus;
import com.ecommerce.order.domain.model.KycStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer_kyc_status")
public class CustomerKycStatusEntity {

    @Id
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private KycStatus status;

    @Column(nullable = false)
    private Instant updatedAt;

    protected CustomerKycStatusEntity() {
    }

    public CustomerKycStatusEntity(UUID userId, KycStatus status, Instant updatedAt) {
        this.userId = userId;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public void setStatus(KycStatus status) {
        this.status = status;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public CustomerKycStatus toDomain() {
        return new CustomerKycStatus(userId, status, updatedAt);
    }
}
