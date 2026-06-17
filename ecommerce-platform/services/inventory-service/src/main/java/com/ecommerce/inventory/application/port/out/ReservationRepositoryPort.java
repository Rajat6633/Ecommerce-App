package com.ecommerce.inventory.application.port.out;

import com.ecommerce.inventory.domain.model.StockReservation;

import java.util.List;
import java.util.UUID;

public interface ReservationRepositoryPort {

    boolean existsByOrderId(UUID orderId);

    List<StockReservation> findByOrderId(UUID orderId);

    void saveAll(List<StockReservation> reservations);

    StockReservation save(StockReservation reservation);
}
