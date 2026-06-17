package com.ecommerce.kyc.infrastructure.ratelimit;

import com.ecommerce.kyc.application.port.out.UploadRateLimiterPort.RateLimitExceededException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Per-user upload rate limit (roadmap C3). Fully offline — no Spring context,
 * no Kafka/DB. Verifies that the limiter is keyed per userId: one user can be
 * throttled while another is unaffected.
 */
class UserUploadRateLimiterTest {

    private UserUploadRateLimiter limiterWith(int perMinute) {
        return new UserUploadRateLimiter(
                new UploadRateLimitProperties(perMinute), new SimpleMeterRegistry());
    }

    @Test
    void allowsUploadsUpToTheLimit() {
        UserUploadRateLimiter limiter = limiterWith(3);
        UUID user = UUID.randomUUID();

        assertThatCode(() -> {
            limiter.acquire(user);
            limiter.acquire(user);
            limiter.acquire(user);
        }).doesNotThrowAnyException();
    }

    @Test
    void sameUserExceedingTheLimitIsRejectedWith429Exception() {
        UserUploadRateLimiter limiter = limiterWith(2);
        UUID user = UUID.randomUUID();

        limiter.acquire(user);
        limiter.acquire(user);

        assertThatThrownBy(() -> limiter.acquire(user))
                .isInstanceOf(RateLimitExceededException.class)
                .hasMessageContaining("rate limit");
    }

    @Test
    void differentUserHasAnIndependentBudget() {
        UserUploadRateLimiter limiter = limiterWith(2);
        UUID throttled = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        // Exhaust the first user's budget.
        limiter.acquire(throttled);
        limiter.acquire(throttled);
        assertThatThrownBy(() -> limiter.acquire(throttled))
                .isInstanceOf(RateLimitExceededException.class);

        // A different user is unaffected by the first user's exhaustion.
        assertThatCode(() -> {
            limiter.acquire(other);
            limiter.acquire(other);
        }).doesNotThrowAnyException();
    }

    @Test
    void misconfiguredLimitFallsBackToSafeDefault() {
        UploadRateLimitProperties props = new UploadRateLimitProperties(0);
        org.assertj.core.api.Assertions.assertThat(props.perUserPerMinute())
                .isEqualTo(UploadRateLimitProperties.DEFAULT_PER_USER_PER_MINUTE);
    }
}
