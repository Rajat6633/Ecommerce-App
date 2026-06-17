package com.ecommerce.auth.infrastructure.messaging;

import org.springframework.context.annotation.Configuration;

/**
 * Kafka configuration for auth-service. <strong>Producer only</strong> —
 * auth-service publishes {@code user.registered} but consumes nothing, so there
 * is no consumer factory, listener container, or dead-letter handler here. The
 * {@code ProducerFactory} / {@code KafkaTemplate} (String key + JSON value) are
 * auto-configured from the {@code spring.kafka.producer.*} properties, matching
 * the other publishers in the platform.
 */
@Configuration
public class KafkaConfig {
}
