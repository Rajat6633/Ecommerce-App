package com.ecommerce.order.api;

import com.ecommerce.order.application.port.out.CartClientPort;
import com.ecommerce.order.application.port.out.CartClientPort.CartSnapshot;
import com.ecommerce.order.application.port.out.CartClientPort.Line;
import com.ecommerce.order.api.dto.OrderResponse;
import com.ecommerce.order.util.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Place-order flow against real Postgres + Kafka (Testcontainers); cart-service
 * is mocked at the port. Requires Docker — runs under failsafe (mvn verify).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderPlacementIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("order_db");

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

    @MockBean
    CartClientPort cartClient;

    @Autowired
    TestRestTemplate rest;

    @Test
    void placeOrder_persistsAndAppearsInHistory() {
        UUID userId = UUID.randomUUID();
        when(cartClient.getCart(any())).thenReturn(
                new CartSnapshot(List.of(new Line(UUID.randomUUID(), 2, new BigDecimal("15.00")))));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TestTokens.mint(userId.toString(), "CUSTOMER"));

        ResponseEntity<OrderResponse> placed = rest.exchange("/api/orders", HttpMethod.POST,
                new HttpEntity<>(headers), OrderResponse.class);
        assertThat(placed.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(placed.getBody().status().name()).isEqualTo("PENDING");
        assertThat(placed.getBody().totalAmount()).isEqualByComparingTo("30.00");

        ResponseEntity<OrderResponse[]> history = rest.exchange("/api/orders", HttpMethod.GET,
                new HttpEntity<>(headers), OrderResponse[].class);
        assertThat(history.getBody()).hasSize(1);
    }

    @Test
    void placeOrder_withoutToken_returns401() {
        ResponseEntity<String> resp = rest.exchange("/api/orders", HttpMethod.POST,
                HttpEntity.EMPTY, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
