package com.ecommerce.kyc.infrastructure.vendor;

import feign.Request;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/** Feign timeouts for the vendor call (connect/read), mirroring order-service. */
public class VendorFeignConfig {

    @Bean
    public Request.Options vendorRequestOptions() {
        return new Request.Options(2, TimeUnit.SECONDS, 5, TimeUnit.SECONDS, true);
    }
}
