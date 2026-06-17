package com.ecommerce.kyc.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.UserRegisteredEvent;
import com.ecommerce.kyc.application.port.in.ScreenCustomerUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Saga entry point. Listens to {@code user.registered} (emitted by auth-service —
 * see docs/19 §3, a stated prerequisite) and opens + screens a KYC case.
 * Idempotent via the use case's processed-events ledger.
 */
@Component
public class UserEventsConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserEventsConsumer.class);

    private final ScreenCustomerUseCase screenCustomer;

    public UserEventsConsumer(ScreenCustomerUseCase screenCustomer) {
        this.screenCustomer = screenCustomer;
    }

    @KafkaListener(topics = Topics.USER_REGISTERED)
    public void onUserRegistered(EventEnvelope<UserRegisteredEvent> envelope) {
        UserRegisteredEvent event = envelope.payload();
        log.info("Received user.registered userId={}", event.userId());
        screenCustomer.onUserRegistered(envelope.eventId(), event.userId(), event.email());
    }
}
