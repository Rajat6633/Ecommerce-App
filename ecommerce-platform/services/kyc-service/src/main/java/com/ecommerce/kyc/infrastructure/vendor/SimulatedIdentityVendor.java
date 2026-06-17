package com.ecommerce.kyc.infrastructure.vendor;

import com.ecommerce.kyc.application.port.out.IdentityVendorPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default vendor adapter: simulates a passing liveness/face-match check without
 * calling any external vendor. Active unless {@code kyc.vendor.enabled=true},
 * mirroring notification-service's LoggingNotificationSender — so local/dev/test
 * need no vendor key.
 */
@Component
@ConditionalOnProperty(name = "kyc.vendor.enabled", havingValue = "false", matchIfMissing = true)
public class SimulatedIdentityVendor implements IdentityVendorPort {

    private static final Logger log = LoggerFactory.getLogger(SimulatedIdentityVendor.class);

    @Override
    public VendorCheck verify(UUID userId) {
        log.info("[SIMULATED-VENDOR] liveness/face-match PASS for user {}", userId);
        return new VendorCheck(true, "sim-" + userId, "simulated vendor check");
    }
}
