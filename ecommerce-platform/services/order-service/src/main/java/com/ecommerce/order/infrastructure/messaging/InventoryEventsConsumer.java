package com.ecommerce.order.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.InventoryReservationFailedEvent;
import com.ecommerce.common.events.payload.InventoryReservedEvent;
import com.ecommerce.order.application.port.in.OrderSagaUseCase;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Consumes inventory saga events to advance/reject the order. */
@Component
public class InventoryEventsConsumer {

    private final OrderSagaUseCase saga;

    public InventoryEventsConsumer(OrderSagaUseCase saga) {
        this.saga = saga;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    public void onReserved(EventEnvelope<InventoryReservedEvent> envelope) {
        saga.onInventoryReserved(envelope.eventId(), envelope.payload().orderId());
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVATION_FAILED)
    public void onReservationFailed(EventEnvelope<InventoryReservationFailedEvent> envelope) {
        InventoryReservationFailedEvent e = envelope.payload();
        saga.onInventoryReservationFailed(envelope.eventId(), e.orderId(), e.reason());
    }
}
