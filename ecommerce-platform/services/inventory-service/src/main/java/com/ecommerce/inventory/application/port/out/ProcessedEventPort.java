package com.ecommerce.inventory.application.port.out;

import java.util.UUID;

/** Consumer idempotency: records processed event ids to suppress duplicates. */
public interface ProcessedEventPort {

    /** @return true if this is the first time the event is seen (and now recorded). */
    boolean firstSeen(UUID eventId);
}
