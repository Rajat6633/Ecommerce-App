package com.ecommerce.common.events.contract;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Builds an {@link ObjectMapper} configured <strong>identically to the one the
 * services use at runtime</strong>, so these contract tests serialize/deserialize
 * exactly like Kafka does on the wire. If this drifts from runtime, the contract
 * becomes fiction.
 *
 * <h2>Why these exact settings</h2>
 * Every producer publishes with Spring Kafka's {@code JsonSerializer} and every
 * consumer reads via {@code ByteArrayJsonMessageConverter(objectMapper)} (see each
 * service's {@code KafkaConfig} + {@code application.yml}). Both are wired with the
 * <em>Spring-managed, Boot-auto-configured</em> {@code ObjectMapper}, which:
 * <ul>
 *   <li>registers {@link JavaTimeModule} (so {@code java.time.Instant} works), and</li>
 *   <li>disables {@link SerializationFeature#WRITE_DATES_AS_TIMESTAMPS}
 *       (Boot's default) &rarr; {@code Instant} is written as an ISO-8601 string,
 *       not a numeric epoch.</li>
 * </ul>
 * No service registers a custom Jackson module or feature for events, so this is a
 * faithful stand-in for the runtime mapper. {@code spring.json.add.type.headers}
 * is {@code false} platform-wide and the consumer resolves the target type from the
 * listener method signature, so no {@code __TypeId__}/type metadata appears in the
 * JSON body — matching what we assert here.
 *
 * <p>Future option (out of scope for C6, deliberately not adopted to stay
 * lightweight): Spring Cloud Contract for stub-driven verification.
 */
final class ContractObjectMapper {

    private ContractObjectMapper() {
    }

    static ObjectMapper create() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
