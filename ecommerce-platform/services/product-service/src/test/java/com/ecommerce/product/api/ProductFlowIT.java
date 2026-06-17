package com.ecommerce.product.api;

import com.ecommerce.product.api.dto.CategoryResponse;
import com.ecommerce.product.api.dto.CreateCategoryRequest;
import com.ecommerce.product.api.dto.CreateProductRequest;
import com.ecommerce.product.api.dto.ProductResponse;
import com.ecommerce.product.util.TestTokens;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end catalog flow with a real PostgreSQL (Testcontainers) and minted
 * RS256 tokens. Requires Docker — runs under failsafe (mvn verify).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")   // wires StubEmbeddingModel + SimpleVectorStore (offline); no ONNX/pgvector
@Testcontainers
class ProductFlowIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine").withDatabaseName("product_db");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    TestRestTemplate rest;

    private HttpHeaders bearer(String token) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @Test
    void admin_createsCategoryAndProduct_publicCanReadAndSearch() {
        String admin = TestTokens.mint(UUID.randomUUID().toString(), "ADMIN");

        // create category
        var catReq = new HttpEntity<>(new CreateCategoryRequest("Books", null), bearer(admin));
        ResponseEntity<CategoryResponse> cat =
                rest.exchange("/api/categories", HttpMethod.POST, catReq, CategoryResponse.class);
        assertThat(cat.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID categoryId = cat.getBody().id();

        // create product
        var prodReq = new HttpEntity<>(new CreateProductRequest(
                "SKU-1", "Clean Architecture", "book", new BigDecimal("39.99"), "USD", categoryId),
                bearer(admin));
        ResponseEntity<ProductResponse> created =
                rest.exchange("/api/products", HttpMethod.POST, prodReq, ProductResponse.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID productId = created.getBody().id();

        // public read (no token)
        ResponseEntity<ProductResponse> fetched =
                rest.getForEntity("/api/products/" + productId, ProductResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().sku()).isEqualTo("SKU-1");

        // public search by name
        ResponseEntity<String> search =
                rest.getForEntity("/api/products?name=clean&sortBy=price", String.class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(search.getBody()).contains("SKU-1");
    }

    @Test
    void semanticSearchAndRecommendations_workEndToEnd() {
        String admin = TestTokens.mint(UUID.randomUUID().toString(), "ADMIN");

        var catReq = new HttpEntity<>(new CreateCategoryRequest("Electronics", null), bearer(admin));
        UUID categoryId = rest.exchange("/api/categories", HttpMethod.POST, catReq, CategoryResponse.class)
                .getBody().id();

        UUID laptopId = createProduct(admin, "SKU-L1", "Gaming Laptop", "fast computer notebook", categoryId);
        createProduct(admin, "SKU-L2", "Gaming Laptop Air", "light computer notebook", categoryId);
        createProduct(admin, "SKU-B1", "Mystery Novel", "a detective story book", categoryId);

        // D1 — semantic search is public (no token)
        ResponseEntity<String> search =
                rest.getForEntity("/api/products/search?q=gaming%20laptop%20computer", String.class);
        assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(search.getBody()).contains("Gaming Laptop");

        // D5 — recommendations exclude the product itself
        ResponseEntity<String> recs =
                rest.getForEntity("/api/products/" + laptopId + "/recommendations?limit=5", String.class);
        assertThat(recs.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recs.getBody()).doesNotContain(laptopId.toString());
        assertThat(recs.getBody()).contains("Gaming Laptop Air");
    }

    private UUID createProduct(String adminToken, String sku, String name, String desc, UUID categoryId) {
        var req = new HttpEntity<>(new CreateProductRequest(sku, name, desc,
                new BigDecimal("99.99"), "USD", categoryId), bearer(adminToken));
        return rest.exchange("/api/products", HttpMethod.POST, req, ProductResponse.class).getBody().id();
    }

    @Test
    void createProduct_withoutToken_returns401() {
        var req = new HttpEntity<>(new CreateProductRequest(
                "SKU-X", "x", "x", new BigDecimal("1.00"), "USD", UUID.randomUUID()));
        ResponseEntity<String> resp =
                rest.exchange("/api/products", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createProduct_withCustomerRole_returns403() {
        String customer = TestTokens.mint(UUID.randomUUID().toString(), "CUSTOMER");
        var req = new HttpEntity<>(new CreateProductRequest(
                "SKU-Y", "y", "y", new BigDecimal("1.00"), "USD", UUID.randomUUID()), bearer(customer));
        ResponseEntity<String> resp =
                rest.exchange("/api/products", HttpMethod.POST, req, String.class);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
