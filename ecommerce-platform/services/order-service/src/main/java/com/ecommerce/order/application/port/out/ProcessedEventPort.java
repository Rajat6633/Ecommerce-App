package com.ecommerce.order.application.port.out;

import java.util.UUID;

/** Consumer idempotency: records processed event ids to suppress duplicates. */
public interface ProcessedEventPort {
    boolean firstSeen(UUID eventId);
}
