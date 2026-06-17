package com.ecommerce.common.events.contract;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.payload.InventoryReleasedEvent;
import com.ecommerce.common.events.payload.InventoryReservationFailedEvent;
import com.ecommerce.common.events.payload.InventoryReservedEvent;
import com.ecommerce.common.events.payload.KycApprovedEvent;
import com.ecommerce.common.events.payload.KycRejectedEvent;
import com.ecommerce.common.events.payload.OrderConfirmedEvent;
import com.ecommerce.common.events.payload.OrderCreatedEvent;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.common.events.payload.PaymentFailedEvent;
import com.ecommerce.common.events.payload.UserRegisteredEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <h1>C6 — Event contract tests (lightweight, JUnit 5 + Jackson only)</h1>
 *
 * <p>These tests lock the JSON wire-format of every Kafka event payload and prove
 * producer&harr;consumer compatibility, entirely OFFLINE (no Kafka/Docker — pure
 * serialization round-trips). They use a {@link ContractObjectMapper} configured to
 * match the services' runtime {@code ObjectMapper}, so the contract is faithful to
 * what actually travels on the wire.
 *
 * <h2>WHAT A FAILURE HERE MEANS — READ THIS BEFORE "fixing" the test</h2>
 * <ul>
 *   <li><b>Schema-lock test fails</b> &rarr; you <b>changed an event contract</b>: a
 *       field was renamed, removed, added, or retyped, or an {@code Instant} stopped
 *       serializing as an ISO-8601 string. This will <b>silently break every
 *       downstream consumer</b> deserializing that event. Do NOT just regenerate the
 *       snapshot to make it green. Instead: (1) confirm the change is intentional,
 *       (2) update the affected consumers, (3) bump the envelope {@code version}
 *       and/or add a versioning/upcasting strategy, (4) then update the committed
 *       snapshot under {@code src/test/resources/contracts/} on purpose.</li>
 *   <li><b>Round-trip test fails</b> &rarr; producer-serialized JSON no longer
 *       deserializes back into the record on the consumer side (the two sides
 *       disagree). Same discipline applies.</li>
 * </ul>
 *
 * <p>Future option (intentionally NOT used to keep this lightweight + free): Spring
 * Cloud Contract for stub-driven, broker-level verification.
 */
@DisplayName("C6 event contracts (schema lock + producer↔consumer round-trip)")
class EventContractTest {

    // ---- Deterministic sample data (must mirror the committed snapshots) ----------

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ORDER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PRODUCT_A = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PRODUCT_B = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID PAYMENT_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static UserRegisteredEvent makeUserRegistered() {
        return new UserRegisteredEvent(USER_ID, "alice@example.com");
    }

    private static KycApprovedEvent makeKycApproved() {
        return new KycApprovedEvent(USER_ID);
    }

    private static KycRejectedEvent makeKycRejected() {
        return new KycRejectedEvent(USER_ID, "Sanctions list match");
    }

    private static OrderCreatedEvent makeOrderCreated() {
        return new OrderCreatedEvent(ORDER_ID, USER_ID, "USD", new BigDecimal("149.99"),
                List.of(new OrderCreatedEvent.OrderLineItem(PRODUCT_A, 2, new BigDecimal("49.99")),
                        new OrderCreatedEvent.OrderLineItem(PRODUCT_B, 1, new BigDecimal("50.01"))));
    }

    private static OrderConfirmedEvent makeOrderConfirmed() {
        return new OrderConfirmedEvent(ORDER_ID, USER_ID);
    }

    private static InventoryReservedEvent makeInventoryReserved() {
        return new InventoryReservedEvent(ORDER_ID, new BigDecimal("149.99"), "USD",
                List.of(new InventoryReservedEvent.ReservedLine(PRODUCT_A, 2),
                        new InventoryReservedEvent.ReservedLine(PRODUCT_B, 1)));
    }

    private static InventoryReservationFailedEvent makeInventoryReservationFailed() {
        return new InventoryReservationFailedEvent(ORDER_ID,
                "Insufficient stock for product 33333333-3333-3333-3333-333333333333");
    }

    private static InventoryReleasedEvent makeInventoryReleased() {
        return new InventoryReleasedEvent(ORDER_ID);
    }

    private static PaymentCompletedEvent makePaymentCompleted() {
        return new PaymentCompletedEvent(ORDER_ID, PAYMENT_ID, new BigDecimal("149.99"), "USD");
    }

    private static PaymentFailedEvent makePaymentFailed() {
        return new PaymentFailedEvent(ORDER_ID, "Card declined");
    }

    // ================================================================================
    // 1. SCHEMA LOCK — exact field set + types per payload (drift guard)
    // ================================================================================
    @Nested
    @DisplayName("Schema lock: serialized JSON must match the committed snapshot exactly")
    class SchemaLock {

        @Test
        void userRegistered() {
            Contracts.assertSchemaLocked("UserRegisteredEvent", makeUserRegistered());
        }

        @Test
        void kycApproved() {
            Contracts.assertSchemaLocked("KycApprovedEvent", makeKycApproved());
        }

        @Test
        void kycRejected() {
            Contracts.assertSchemaLocked("KycRejectedEvent", makeKycRejected());
        }

        @Test
        void orderCreated() {
            Contracts.assertSchemaLocked("OrderCreatedEvent", makeOrderCreated());
        }

        @Test
        void orderConfirmed() {
            Contracts.assertSchemaLocked("OrderConfirmedEvent", makeOrderConfirmed());
        }

        @Test
        void inventoryReserved() {
            Contracts.assertSchemaLocked("InventoryReservedEvent", makeInventoryReserved());
        }

        @Test
        void inventoryReservationFailed() {
            Contracts.assertSchemaLocked("InventoryReservationFailedEvent", makeInventoryReservationFailed());
        }

        @Test
        void inventoryReleased() {
            Contracts.assertSchemaLocked("InventoryReleasedEvent", makeInventoryReleased());
        }

        @Test
        void paymentCompleted() {
            Contracts.assertSchemaLocked("PaymentCompletedEvent", makePaymentCompleted());
        }

        @Test
        void paymentFailed() {
            Contracts.assertSchemaLocked("PaymentFailedEvent", makePaymentFailed());
        }

        @Test
        @DisplayName("EventEnvelope<T>: envelope shape + Instant as ISO-8601 string")
        void eventEnvelope() {
            EventEnvelope<KycApprovedEvent> envelope = new EventEnvelope<>(
                    UUID.fromString("99999999-9999-9999-9999-999999999999"),
                    "kyc.approved", 1,
                    Instant.parse("2026-06-15T10:15:30Z"),
                    "0af7651916cd43dd8448eb211c80319c", "b7ad6b7169203331",
                    makeKycApproved());
            Contracts.assertSchemaLocked("EventEnvelope", envelope);
        }
    }

    // ================================================================================
    // 2. PRODUCER↔CONSUMER ROUND-TRIP — serialize then deserialize, assert equality
    // ================================================================================
    @Nested
    @DisplayName("Round-trip: producer-serialized JSON deserializes back equal on the consumer")
    class RoundTrip {

        @Test
        void userRegistered() {
            Contracts.assertRoundTrip(makeUserRegistered(), UserRegisteredEvent.class);
        }

        @Test
        void kycApproved() {
            Contracts.assertRoundTrip(makeKycApproved(), KycApprovedEvent.class);
        }

        @Test
        void kycRejected() {
            Contracts.assertRoundTrip(makeKycRejected(), KycRejectedEvent.class);
        }

        @Test
        void orderCreated() {
            Contracts.assertRoundTrip(makeOrderCreated(), OrderCreatedEvent.class);
        }

        @Test
        void orderConfirmed() {
            Contracts.assertRoundTrip(makeOrderConfirmed(), OrderConfirmedEvent.class);
        }

        @Test
        void inventoryReserved() {
            Contracts.assertRoundTrip(makeInventoryReserved(), InventoryReservedEvent.class);
        }

        @Test
        void inventoryReservationFailed() {
            Contracts.assertRoundTrip(makeInventoryReservationFailed(), InventoryReservationFailedEvent.class);
        }

        @Test
        void inventoryReleased() {
            Contracts.assertRoundTrip(makeInventoryReleased(), InventoryReleasedEvent.class);
        }

        @Test
        void paymentCompleted() {
            Contracts.assertRoundTrip(makePaymentCompleted(), PaymentCompletedEvent.class);
        }

        @Test
        void paymentFailed() {
            Contracts.assertRoundTrip(makePaymentFailed(), PaymentFailedEvent.class);
        }
    }

    // ================================================================================
    // 3. ENVELOPE GENERIC ROUND-TRIP — the exact generic path ByteArrayJsonMessage-
    //    Converter takes: producer serializes EventEnvelope<Payload>, consumer reads
    //    it back as EventEnvelope<ConcretePayload> via a parameterized TypeReference.
    // ================================================================================
    @Nested
    @DisplayName("Envelope generic round-trip (the ByteArrayJsonMessageConverter path)")
    class EnvelopeRoundTrip {

        @Test
        @DisplayName("EventEnvelope<KycApprovedEvent> survives serialize→deserialize with payload intact")
        void kycApprovedEnvelope() throws Exception {
            EventEnvelope<KycApprovedEvent> produced =
                    EventEnvelope.create("kyc.approved", "trace-1", "corr-1", makeKycApproved());

            String json = Contracts.MAPPER.writeValueAsString(produced);
            EventEnvelope<KycApprovedEvent> consumed = Contracts.MAPPER.readValue(
                    json, new TypeReference<EventEnvelope<KycApprovedEvent>>() {
                    });

            assertEquals(produced, consumed);
            // The consumer reads eventId / occurredAt / payload off the envelope.
            assertEquals(produced.eventId(), consumed.eventId());
            assertEquals(produced.occurredAt(), consumed.occurredAt());
            assertEquals(makeKycApproved(), consumed.payload());
        }

        @Test
        @DisplayName("EventEnvelope<OrderCreatedEvent> with nested line items round-trips")
        void orderCreatedEnvelope() throws Exception {
            EventEnvelope<OrderCreatedEvent> produced =
                    EventEnvelope.create("order.created", "trace-2", "corr-2", makeOrderCreated());

            String json = Contracts.MAPPER.writeValueAsString(produced);
            EventEnvelope<OrderCreatedEvent> consumed = Contracts.MAPPER.readValue(
                    json, new TypeReference<EventEnvelope<OrderCreatedEvent>>() {
                    });

            assertEquals(produced, consumed);
            assertEquals(makeOrderCreated(), consumed.payload());
            assertEquals(2, consumed.payload().items().size());
        }
    }
}
