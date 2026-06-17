package com.ecommerce.payment.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.InventoryReservedEvent;
import com.ecommerce.payment.application.port.in.ProcessPaymentUseCase;
import com.ecommerce.payment.application.port.in.ProcessPaymentUseCase.PaymentOutcome;
import com.ecommerce.payment.application.port.out.PaymentEventPublisherPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@code inventory.reserved}, processes the payment, then (post-commit)
 * publishes {@code payment.completed} or {@code payment.failed} to close the saga loop.
 */
@Component
public class InventoryEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryEventsConsumer.class);

    private final ProcessPaymentUseCase processPayment;
    private final PaymentEventPublisherPort publisher;

    public InventoryEventsConsumer(ProcessPaymentUseCase processPayment,
                                   PaymentEventPublisherPort publisher) {
        this.processPayment = processPayment;
        this.publisher = publisher;
    }

    @KafkaListener(topics = Topics.INVENTORY_RESERVED)
    public void onInventoryReserved(EventEnvelope<InventoryReservedEvent> envelope) {
        InventoryReservedEvent event = envelope.payload();
        log.info("Received inventory.reserved orderId={} amount={}", event.orderId(), event.amount());

        PaymentOutcome outcome = processPayment.process(
                envelope.eventId(), event.orderId(), event.amount(), event.currency());

        switch (outcome.status()) {
            case COMPLETED -> publisher.publishCompleted(outcome.payment());
            case FAILED -> publisher.publishFailed(event.orderId(), outcome.failureReason());
            case ALREADY_PROCESSED -> log.debug("Duplicate inventory.reserved ignored: {}", event.orderId());
        }
    }
}
