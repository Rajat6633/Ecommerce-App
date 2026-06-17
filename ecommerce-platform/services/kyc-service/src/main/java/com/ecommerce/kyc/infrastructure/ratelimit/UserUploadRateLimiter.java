package com.ecommerce.kyc.infrastructure.ratelimit;

import com.ecommerce.kyc.application.port.out.UploadRateLimiterPort;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Per-user upload rate limiter (roadmap C3). Backed by a Resilience4j
 * {@link RateLimiterRegistry}: each user gets their own registry-keyed limiter
 * ({@code kyc-upload-<userId>}), so one user's bursts can't starve other users.
 * Complements — does not replace — the service-wide {@code kyc-ai} limiter on the
 * extraction call.
 *
 * <p>The limiter is configured to refresh its permits once per minute
 * ({@code kyc.upload.rate-limit.per-user-per-minute}, default 5) and to <em>not</em>
 * block waiting for a permit (timeout 0): when a user is over budget the acquire
 * fails fast and we surface {@link RateLimitExceededException} → HTTP 429. Keeping
 * Resilience4j confined here means neither the api nor the domain layer sees it.
 */
@Component
public class UserUploadRateLimiter implements UploadRateLimiterPort {

    private static final Logger log = LoggerFactory.getLogger(UserUploadRateLimiter.class);

    private final RateLimiterRegistry registry;
    private final RateLimiterConfig perUserConfig;
    private final int permitsPerMinute;

    public UserUploadRateLimiter(UploadRateLimitProperties properties, MeterRegistry meterRegistry) {
        this.permitsPerMinute = properties.perUserPerMinute();
        this.perUserConfig = RateLimiterConfig.custom()
                .limitForPeriod(permitsPerMinute)
                .limitRefreshPeriod(Duration.ofMinutes(1))
                .timeoutDuration(Duration.ZERO) // fail fast — do not queue uploads
                .build();
        this.registry = RateLimiterRegistry.ofDefaults();
        meterRegistry.counter("kyc_upload_rate_limited_total");
        log.info("Per-user KYC upload limiter active: {} upload(s)/minute/user", permitsPerMinute);
    }

    @Override
    public void acquire(UUID userId) {
        RateLimiter limiter = registry.rateLimiter("kyc-upload-" + userId, perUserConfig);
        if (!limiter.acquirePermission()) {
            log.warn("Per-user upload rate limit exceeded for user {} ({}/min)", userId, permitsPerMinute);
            throw new RateLimitExceededException(
                    "Upload rate limit exceeded: max " + permitsPerMinute
                            + " document upload(s) per minute. Please retry shortly.");
        }
    }
}
