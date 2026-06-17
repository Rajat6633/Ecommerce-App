package com.ecommerce.auth.infrastructure.messaging;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.UserRegisteredEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Focused unit test for the publisher adapter — mocks {@link KafkaTemplate}, so
 * it needs no running broker. With no active transaction the send happens
 * inline; the topic, key and envelope/payload must match what kyc-service's
 * UserEventsConsumer deserializes ({@code EventEnvelope<UserRegisteredEvent>}).
 */
@ExtendWith(MockitoExtension.class)
class UserEventPublisherAdapterTest {

    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    @SuppressWarnings("unchecked")
    void publishUserRegistered_noActiveTransaction_sendsEnvelopeInline() {
        var adapter = new UserEventPublisherAdapter(kafkaTemplate);
        UUID userId = UUID.randomUUID();

        adapter.publishUserRegistered(userId, "a@b.com");

        ArgumentCaptor<String> topic = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> value = ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(topic.capture(), key.capture(), value.capture());

        assertThat(topic.getValue()).isEqualTo(Topics.USER_REGISTERED);
        assertThat(key.getValue()).isEqualTo(userId.toString());

        assertThat(value.getValue()).isInstanceOf(EventEnvelope.class);
        EventEnvelope<UserRegisteredEvent> envelope = (EventEnvelope<UserRegisteredEvent>) value.getValue();
        assertThat(envelope.eventType()).isEqualTo(Topics.USER_REGISTERED);
        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.payload()).isEqualTo(new UserRegisteredEvent(userId, "a@b.com"));
    }
}
