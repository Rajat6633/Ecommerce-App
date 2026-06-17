package com.ecommerce.notification.saga;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.OrderConfirmedEvent;
import com.ecommerce.common.events.payload.PaymentCompletedEvent;
import com.ecommerce.notification.application.port.in.NotificationQueryUseCase;
import com.ecommerce.notification.domain.model.Notification;
import com.ecommerce.notification.domain.model.NotificationStatus;
import com.ecommerce.notification.domain.model.NotificationType;
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
 * Publish order.confirmed + payment.completed → assert the notification-service
 * consumes both, dispatches (simulated sender), and persists two SENT audit
 * rows keyed by orderId. Requires Docker (Postgres + Kafka).
 */
@SpringBootTest
@Testcontainers
class NotificationSagaIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("notification_db");

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
    NotificationQueryUseCase notifications;

    @Test
    void confirmedAndPaid_producesTwoSentNotifications() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(Topics.ORDER_CONFIRMED, orderId.toString(),
                EventEnvelope.create(Topics.ORDER_CONFIRMED, null, null,
                        new OrderConfirmedEvent(orderId, userId)));
        kafkaTemplate.send(Topics.PAYMENT_COMPLETED, orderId.toString(),
                EventEnvelope.create(Topics.PAYMENT_COMPLETED, null, null,
                        new PaymentCompletedEvent(orderId, UUID.randomUUID(),
                                new BigDecimal("39.98"), "USD")));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            List<Notification> rows = notifications.getByReference(orderId);
            assertThat(rows).hasSize(2);
            assertThat(rows).allSatisfy(n ->
                    assertThat(n.status()).isEqualTo(NotificationStatus.SENT));
            assertThat(rows).extracting(Notification::type)
                    .containsExactlyInAnyOrder(
                            NotificationType.ORDER_CONFIRMED, NotificationType.PAYMENT_COMPLETED);
        });
    }
}
