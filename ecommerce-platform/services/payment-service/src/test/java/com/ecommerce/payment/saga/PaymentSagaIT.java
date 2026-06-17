package com.ecommerce.payment.saga;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.InventoryReservedEvent;
import com.ecommerce.payment.application.port.in.PaymentQueryUseCase;
import com.ecommerce.payment.domain.model.PaymentStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Publish inventory.reserved → assert a COMPLETED payment is persisted
 * (success-rate=1.0 in the test profile). Requires Docker (Postgres + Kafka).
 */
@SpringBootTest
@Testcontainers
class PaymentSagaIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("payment_db");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    KafkaTemplate<String, Object> kafkaTemplate;
    @Autowired
    PaymentQueryUseCase payments;

    @Test
    void inventoryReserved_producesCompletedPayment() {
        UUID orderId = UUID.randomUUID();
        var payload = new InventoryReservedEvent(orderId, new BigDecimal("39.98"), "USD",
                List.of(new InventoryReservedEvent.ReservedLine(UUID.randomUUID(), 2)));
        kafkaTemplate.send(Topics.INVENTORY_RESERVED, orderId.toString(),
                EventEnvelope.create(Topics.INVENTORY_RESERVED, null, null, payload));

        // ignoreExceptions(): getByOrderId throws PaymentNotFoundException until the
        // saga persists the payment; without this, untilAsserted (which only swallows
        // AssertionError) would fail fast on the first poll instead of retrying.
        await().ignoreExceptions().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(payments.getByOrderId(orderId).status()).isEqualTo(PaymentStatus.COMPLETED));
    }
}
