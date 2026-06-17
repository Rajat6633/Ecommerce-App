package com.ecommerce.inventory.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.PaymentFailedEvent;
import com.ecommerce.inventory.application.port.in.StockReservationUseCase;
import com.ecommerce.inventory.application.port.out.InventoryEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Compensation: on {@code payment.failed}, release the order's reservation and
 * (post-commit) publish {@code inventory.released}.
 */
@Component
public class PaymentEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventsConsumer.class);

    private final StockReservationUseCase reservationUseCase;
    private final InventoryEventPublisherPort publisher;

    public PaymentEventsConsumer(StockReservationUseCase reservationUseCase,
                                 InventoryEventPublisherPort publisher) {
        this.reservationUseCase = reservationUseCase;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.PAYMENT_FAILED)
    public void onPaymentFailed(EventEnvelope<PaymentFailedEvent> envelope) {
        PaymentFailedEvent event = envelope.payload();
        log.info("Received payment.failed orderId={} reason={}", event.orderId(), event.reason());
        boolean released = reservationUseCase.releaseForOrder(envelope.eventId(), event.orderId());
        if (released) {
            publisher.publishReleased(event.orderId());
        }
    }
}
