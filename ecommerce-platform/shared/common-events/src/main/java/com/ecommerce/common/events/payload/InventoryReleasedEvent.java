package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by inventory-service after releasing a reservation (compensation done). */
public record InventoryReleasedEvent(
        UUID orderId
) {
}
