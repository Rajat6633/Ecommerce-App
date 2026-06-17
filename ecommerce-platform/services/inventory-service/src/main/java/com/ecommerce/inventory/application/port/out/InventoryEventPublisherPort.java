package com.ecommerce.inventory.application.port.out;

import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservedItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Outbound port for emitting inventory saga events to Kafka. */
public interface InventoryEventPublisherPort {

    void publishReserved(UUID orderId, BigDecimal amount, String currency, List<ReservedItem> items);

    void publishReservationFailed(UUID orderId, String reason);

    void publishReleased(UUID orderId);
}
