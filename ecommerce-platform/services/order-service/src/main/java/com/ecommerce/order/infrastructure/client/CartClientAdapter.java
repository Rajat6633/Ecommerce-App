package com.ecommerce.order.infrastructure.client;

import com.ecommerce.order.application.port.out.CartClientPort;
import com.ecommerce.order.domain.exception.CartServiceUnavailableException;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Resilient adapter over {@link CartFeignClient}. The user is implied by the
 * propagated JWT (cart-service derives it from the token subject), so
 * {@code userId} is used only for logging. A downstream outage on the critical
 * {@code getCart} path surfaces as {@link CartServiceUnavailableException} (503).
 *
 * <p>Full Resilience4j stack on the critical read (applied in aspect order
 * {@code Retry → CircuitBreaker → RateLimiter → Bulkhead}): retries transient
 * faults, opens the circuit on sustained failure, rate-limits the outbound call
 * rate, and caps concurrent in-flight calls so a slow cart-service cannot
 * exhaust request threads. Local back-pressure ({@code RequestNotPermitted} /
 * {@code BulkheadFullException}) is funnelled through the same fallback and is
 * configured as an ignored exception so it never trips the circuit. Call
 * timeouts are enforced at the Feign layer (connect/read timeouts).
 */
@Component
public class CartClientAdapter implements CartClientPort {

    private static final Logger log = LoggerFactory.getLogger(CartClientAdapter.class);
    private static final String CB = "cart-service";

    private final CartFeignClient client;

    public CartClientAdapter(CartFeignClient client) {
        this.client = client;
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "getCartFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public CartSnapshot getCart(UUID userId) {
        CartDto dto = client.getCart();
        List<Line> lines = dto.items() == null ? List.of() : dto.items().stream()
                .map(i -> new Line(i.productId(), i.quantity(), i.unitPrice()))
                .toList();
        return new CartSnapshot(lines);
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB)
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public void clear(UUID userId) {
        client.clearCart();
    }

    @SuppressWarnings("unused")
    private CartSnapshot getCartFallback(UUID userId, Throwable t) {
        log.warn("cart-service unavailable for user {} ({})", userId, t.toString());
        throw new CartServiceUnavailableException();
    }
}
