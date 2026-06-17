package com.ecommerce.auth.api;

import com.ecommerce.auth.api.dto.AuthResponse;
import com.ecommerce.auth.api.dto.LoginRequest;
import com.ecommerce.auth.api.dto.RefreshTokenRequest;
import com.ecommerce.auth.api.dto.RegisterRequest;
import com.ecommerce.auth.api.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end auth flow against a real PostgreSQL (Testcontainers) + Flyway.
 * Requires Docker — runs in the integration-test (failsafe) phase, not surefire.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("auth_db");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("management.tracing.sampling.probability", () -> "0.0");
    }

    @Autowired
    TestRestTemplate rest;

    @Test
    void register_login_me_refresh_happyPath() {
        // 1. Register
        var register = rest.postForEntity("/api/auth/register",
                new RegisterRequest("alice@example.com", "password123", "Alice"), UserResponse.class);
        assertThat(register.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(register.getBody()).isNotNull();
        assertThat(register.getBody().email()).isEqualTo("alice@example.com");

        // 2. Login
        var login = rest.postForEntity("/api/auth/login",
                new LoginRequest("alice@example.com", "password123"), AuthResponse.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(login.getBody()).isNotNull();
        String accessToken = login.getBody().accessToken();
        String refreshToken = login.getBody().refreshToken();
        assertThat(accessToken).isNotBlank();

        // 3. /me with bearer token
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<UserResponse> me = rest.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), UserResponse.class);
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).isNotNull();
        assertThat(me.getBody().email()).isEqualTo("alice@example.com");

        // 4. Refresh -> new tokens
        var refreshed = rest.postForEntity("/api/auth/refresh",
                new RefreshTokenRequest(refreshToken), AuthResponse.class);
        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshed.getBody()).isNotNull();
        assertThat(refreshed.getBody().refreshToken()).isNotEqualTo(refreshToken); // rotated
    }

    @Test
    void me_withoutToken_returns401() {
        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = rest.exchange("/api/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_wrongPassword_returns401() {
        rest.postForEntity("/api/auth/register",
                new RegisterRequest("bob@example.com", "password123", "Bob"), UserResponse.class);
        ResponseEntity<String> resp = rest.postForEntity("/api/auth/login",
                new LoginRequest("bob@example.com", "wrongpass"), String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
