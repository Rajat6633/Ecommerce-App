package com.ecommerce.kyc;

import com.ecommerce.kyc.application.service.KycProperties;
import com.ecommerce.kyc.infrastructure.ratelimit.UploadRateLimitProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties({KycProperties.class, UploadRateLimitProperties.class})
public class KycServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(KycServiceApplication.class, args);
    }
}
