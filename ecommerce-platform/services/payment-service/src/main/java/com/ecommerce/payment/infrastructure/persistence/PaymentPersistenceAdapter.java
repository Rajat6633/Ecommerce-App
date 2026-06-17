package com.ecommerce.payment.infrastructure.persistence;

import com.ecommerce.payment.application.port.out.PaymentRepositoryPort;
import com.ecommerce.payment.domain.model.Payment;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class PaymentPersistenceAdapter implements PaymentRepositoryPort {

    private final PaymentJpaRepository repository;

    public PaymentPersistenceAdapter(PaymentJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return repository.existsByOrderId(orderId);
    }

    @Override
    public Optional<Payment> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).map(PaymentEntity::toDomain);
    }

    @Override
    public Payment save(Payment payment) {
        return repository.save(PaymentEntity.fromDomain(payment)).toDomain();
    }
}
