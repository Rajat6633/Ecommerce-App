package com.ecommerce.cart.infrastructure.client;

import com.ecommerce.cart.application.port.out.ProductCatalogPort;
import com.ecommerce.cart.domain.exception.ProductServiceUnavailableException;
import feign.FeignException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Resilient adapter over {@link ProductFeignClient}. A 404 is a normal "absent"
 * answer (empty). Transient failures are retried, then the circuit breaker
 * opens and {@link #findProductFallback} converts the outage into a clean
 * {@link ProductServiceUnavailableException} (HTTP 503).
 *
 * <p>Full Resilience4j stack (aspect order {@code Retry → CircuitBreaker →
 * RateLimiter → Bulkhead}): the rate limiter caps the outbound call rate and
 * the bulkhead caps concurrent in-flight calls so a slow product-service cannot
 * exhaust request threads. Local back-pressure ({@code RequestNotPermitted} /
 * {@code BulkheadFullException}) routes through the same fallback and is ignored
 * by the circuit breaker. Timeouts are enforced at the Feign layer.
 */
@Component
public class ProductCatalogAdapter implements ProductCatalogPort {

    private static final Logger log = LoggerFactory.getLogger(ProductCatalogAdapter.class);
    private static final String CB = "product-service";

    private final ProductFeignClient client;

    public ProductCatalogAdapter(ProductFeignClient client) {
        this.client = client;
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "findProductFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public Optional<ProductInfo> findProduct(UUID productId) {
        try {
            ProductDto p = client.getById(productId);
            return Optional.of(new ProductInfo(p.id(), p.price(), p.currency(), p.active()));
        } catch (FeignException.NotFound notFound) {
            return Optional.empty(); // business "absent" — not a circuit failure
        }
    }

    @SuppressWarnings("unused") // invoked by Resilience4j on failure / open circuit
    private Optional<ProductInfo> findProductFallback(UUID productId, Throwable t) {
        log.warn("product-service lookup failed for {} ({}); returning unavailable",
                productId, t.toString());
        throw new ProductServiceUnavailableException();
    }
}
