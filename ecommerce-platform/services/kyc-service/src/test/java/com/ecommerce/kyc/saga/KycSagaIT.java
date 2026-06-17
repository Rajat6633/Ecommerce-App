package com.ecommerce.kyc.saga;

import com.ecommerce.common.events.EventEnvelope;
import com.ecommerce.common.events.Topics;
import com.ecommerce.common.events.payload.UserRegisteredEvent;
import com.ecommerce.kyc.application.port.in.KycQueryUseCase;
import com.ecommerce.kyc.domain.model.KycCase;
import com.ecommerce.kyc.domain.model.KycStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Deferred (failsafe *IT tier; needs Docker). Publish {@code user.registered}
 * → assert a {@code kyc_cases} row is created and the case is APPROVED (clean
 * screen with the stub AI models) so {@code kyc.approved} would be published.
 *
 * <p>Runs under the {@code test} profile, so the stub ChatModel/EmbeddingModel +
 * SimpleVectorStore are active (no ANTHROPIC_API_KEY needed). The Postgres
 * image must have pgvector for the real-store path; with the {@code test}
 * profile's SimpleVectorStore that is not required, but the schema migration
 * (V1) still runs against Postgres — use a pgvector-enabled image.
 *
 * <p>NOTE: the {@code user.registered} producer in auth-service does not exist
 * yet (docs/19 §3). This IT publishes the expected envelope shape directly.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class KycSagaIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg16")
                    .asCompatibleSubstituteFor("postgres"))
                    .withDatabaseName("kyc_db");

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
    KycQueryUseCase kycQuery;

    @Test
    void userRegistered_createsCaseAndScreens() {
        UUID userId = UUID.randomUUID();

        kafkaTemplate.send(Topics.USER_REGISTERED, userId.toString(),
                EventEnvelope.create(Topics.USER_REGISTERED, null, null,
                        new UserRegisteredEvent(userId, "clean.customer@example.com")));

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            KycCase kycCase = kycQuery.getByUserId(userId);
            assertThat(kycCase.userId()).isEqualTo(userId);
            assertThat(kycCase.status()).isIn(KycStatus.APPROVED, KycStatus.MANUAL_REVIEW);
        });
    }
}
