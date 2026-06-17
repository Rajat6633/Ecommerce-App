package com.ecommerce.inventory.saga;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderCreatedEvent;
import com.ecommerce.common.events.payload.OrderCreatedEvent.OrderLineItem;
import com.ecommerce.inventory.application.port.in.InventoryAdminUseCase;
import com.ecommerce.inventory.infrastructure.persistence.StockReservationJpaRepository;
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
 * End-to-end saga slice: publish {@code order.created}, assert the consumer
 * reserves stock and persists the reservation. Requires Docker (Postgres +
 * Kafka via Testcontainers) — runs under failsafe (mvn verify).
 */
@SpringBootTest
@Testcontainers
class InventorySagaIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("inventory_db");

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
    InventoryAdminUseCase inventory;
    @Autowired
    StockReservationJpaRepository reservations;

    @Test
    void orderCreated_reservesStock() {
        UUID productId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        inventory.upsertStock(productId, 100, 10);

        var payload = new OrderCreatedEvent(orderId, UUID.randomUUID(), "USD",
                new BigDecimal("20.00"), List.of(new OrderLineItem(productId, 3, new BigDecimal("6.66"))));
        kafkaTemplate.send(Topics.ORDER_CREATED, orderId.toString(),
                EventEnvelope.create(Topics.ORDER_CREATED, null, null, payload));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(reservations.existsByOrderId(orderId)).isTrue());

        assertThat(inventory.getByProduct(productId).reserved()).isEqualTo(3);
    }
}
