package com.ecommerce.inventory.infrastructure.persistence;

import com.ecommerce.inventory.application.port.out.ReservationRepositoryPort;
import com.ecommerce.inventory.domain.model.StockReservation;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ReservationPersistenceAdapter implements ReservationRepositoryPort {

    private final StockReservationJpaRepository repository;

    public ReservationPersistenceAdapter(StockReservationJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsByOrderId(UUID orderId) {
        return repository.existsByOrderId(orderId);
    }

    @Override
    public List<StockReservation> findByOrderId(UUID orderId) {
        return repository.findByOrderId(orderId).stream().map(StockReservationEntity::toDomain).toList();
    }

    @Override
    public void saveAll(List<StockReservation> reservations) {
        repository.saveAll(reservations.stream().map(StockReservationEntity::fromDomain).toList());
    }

    @Override
    public StockReservation save(StockReservation reservation) {
        return repository.save(StockReservationEntity.fromDomain(reservation)).toDomain();
    }
}
