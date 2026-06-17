package com.ecommerce.cart.api;

import com.ecommerce.cart.api.dto.AddItemRequest;
import com.ecommerce.cart.api.dto.CartResponse;
import com.ecommerce.cart.application.port.out.ProductCatalogPort;
import com.ecommerce.cart.application.port.out.ProductCatalogPort.ProductInfo;
import com.ecommerce.cart.util.TestTokens;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cart flow with real PostgreSQL (Testcontainers); product-service is mocked at
 * the port boundary so the test is hermetic. Requires Docker — runs under failsafe.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class CartFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("cart_db");

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @MockBean
    ProductCatalogPort productCatalog;

    @Autowired
    TestRestTemplate rest;

    @Test
    void addItem_thenViewCart() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productCatalog.findProduct(any()))
                .thenReturn(Optional.of(new ProductInfo(productId, new BigDecimal("12.50"), "USD", true)));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(TestTokens.mint(userId.toString(), "CUSTOMER"));

        ResponseEntity<CartResponse> added = rest.exchange("/api/cart/items", HttpMethod.POST,
                new HttpEntity<>(new AddItemRequest(productId, 2), headers), CartResponse.class);
        assertThat(added.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(added.getBody().items()).hasSize(1);
        assertThat(added.getBody().totalAmount()).isEqualByComparingTo("25.00");

        ResponseEntity<CartResponse> view = rest.exchange("/api/cart", HttpMethod.GET,
                new HttpEntity<>(headers), CartResponse.class);
        assertThat(view.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(view.getBody().items()).hasSize(1);
    }

    @Test
    void addItem_withoutToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange("/api/cart/items", HttpMethod.POST,
                new HttpEntity<>(new AddItemRequest(UUID.randomUUID(), 1), headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
