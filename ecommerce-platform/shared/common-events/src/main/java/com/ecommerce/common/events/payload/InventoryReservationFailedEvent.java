package com.ecommerce.common.events.payload;

import java.util.UUID;

/** Emitted by inventory-service when stock cannot be reserved (compensation trigger). */
public record InventoryReservationFailedEvent(
        UUID orderId,
        String reason
) {
}
