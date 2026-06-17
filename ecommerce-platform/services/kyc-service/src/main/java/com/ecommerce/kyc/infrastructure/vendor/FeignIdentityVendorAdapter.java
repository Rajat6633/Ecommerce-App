package com.ecommerce.kyc.infrastructure.vendor;

import com.ecommerce.kyc.application.port.out.IdentityVendorPort;
import com.ecommerce.kyc.infrastructure.vendor.IdentityVendorFeignClient.VendorCheckRequest;
import com.ecommerce.kyc.infrastructure.vendor.IdentityVendorFeignClient.VendorCheckResponse;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Real vendor adapter, active only when {@code kyc.vendor.enabled=true}. Wraps
 * the Feign client in the platform-standard Resilience4j stack and fails closed:
 * any vendor outage surfaces as a failed check, routing the case to review.
 */
@Component
@ConditionalOnProperty(name = "kyc.vendor.enabled", havingValue = "true")
public class FeignIdentityVendorAdapter implements IdentityVendorPort {

    private static final Logger log = LoggerFactory.getLogger(FeignIdentityVendorAdapter.class);
    private static final String CB = "identity-vendor";

    private final IdentityVendorFeignClient client;

    public FeignIdentityVendorAdapter(IdentityVendorFeignClient client) {
        this.client = client;
    }

    @Override
    @Retry(name = CB)
    @CircuitBreaker(name = CB, fallbackMethod = "verifyFallback")
    @RateLimiter(name = CB)
    @Bulkhead(name = CB)
    public VendorCheck verify(UUID userId) {
        VendorCheckResponse r = client.runCheck(new VendorCheckRequest(userId.toString()));
        return new VendorCheck(r.passed(), r.reference(), r.detail());
    }

    @SuppressWarnings("unused")
    private VendorCheck verifyFallback(UUID userId, Throwable t) {
        log.warn("Identity vendor unavailable for user {} ({}) — failing closed", userId, t.toString());
        return VendorCheck.failClosed("vendor unavailable: " + t.getMessage());
    }
}
