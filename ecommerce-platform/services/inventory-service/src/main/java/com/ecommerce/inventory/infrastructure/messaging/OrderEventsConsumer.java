package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderCreatedEvent;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservationLine;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase.ReservationOutcome;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Consumes {@code order.created}, reserves stock, then (post-commit) publishes
 * either {@code inventory.reserved} or {@code inventory.reservation-failed}.
 */
@Component
public class OrderEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventsConsumer.class);

    private final StockReservationUseCase reservationUseCase;
    private final InventoryEventPublisherPort publisher;

    public OrderEventsConsumer(StockReservationUseCase reservationUseCase,
                               InventoryEventPublisherPort publisher) {
        this.reservationUseCase = reservationUseCase;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.ORDER_CREATED)
    public void onOrderCreated(EventEnvelope<OrderCreatedEvent> envelope) {
        OrderCreatedEvent event = envelope.payload();
        log.info("Received order.created orderId={} eventId={}", event.orderId(), envelope.eventId());

        List<ReservationLine> lines = event.items().stream()
                .map(i -> new ReservationLine(i.productId(), i.quantity()))
                .toList();

        ReservationOutcome outcome =
                reservationUseCase.reserveForOrder(envelope.eventId(), event.orderId(), lines);

        switch (outcome.status()) {
            case RESERVED -> publisher.publishReserved(event.orderId(), event.totalAmount(),
                    event.currency(), outcome.reservedItems());
            case FAILED -> publisher.publishReservationFailed(event.orderId(), outcome.failureReason());
            case ALREADY_PROCESSED -> log.debug("Duplicate order.created ignored: {}", event.orderId());
        }
    }
}
