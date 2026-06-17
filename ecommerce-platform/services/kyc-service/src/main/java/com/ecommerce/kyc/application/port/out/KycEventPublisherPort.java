package com.ecommerce.kyc.application.port.out;

import java.util.UUID;

/** Outbound saga events. Mirrors order-service's resilient Kafka publisher. */
public interface KycEventPublisherPort {

    void publishApproved(UUID userId);

    void publishRejected(UUID userId, String reason);
}
