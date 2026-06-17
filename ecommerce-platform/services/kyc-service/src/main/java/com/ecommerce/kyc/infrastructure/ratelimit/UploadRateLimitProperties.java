package com.ecommerce.kyc.infrastructure.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tunables for the per-user document-upload rate limit (roadmap C3).
 * {@code perUserPerMinute} is the number of uploads a single user may perform per
 * minute before further uploads are rejected with HTTP 429; defaults to a small
 * value (5) and is coerced to a sane positive number if mis-configured.
 */
@ConfigurationProperties(prefix = "kyc.upload.rate-limit")
public record UploadRateLimitProperties(
        int perUserPerMinute
) {
    public static final int DEFAULT_PER_USER_PER_MINUTE = 5;

    public UploadRateLimitProperties {
        if (perUserPerMinute <= 0) {
            perUserPerMinute = DEFAULT_PER_USER_PER_MINUTE;
        }
    }
}
