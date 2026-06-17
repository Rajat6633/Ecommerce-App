package com.ecommerce.inventory.application.port.in;

import java.util.List;
import java.util.UUID;

/**
 * Saga participant: reserve stock when an order is created, release it on
 * payment failure. Methods return an {@link ReservationOutcome} so the caller
 * (Kafka consumer) can publish the appropriate event AFTER the DB transaction
 * commits — keeping the event consistent with persisted state.
 */
public interface StockReservationUseCase {

    ReservationOutcome reserveForOrder(UUID eventId, UUID orderId, List<ReservationLine> lines);

    /** @return true if a reservation was released (false = nothing to do / already released). */
    boolean releaseForOrder(UUID eventId, UUID orderId);

    record ReservationLine(UUID productId, int quantity) {}

    record ReservedItem(UUID productId, int quantity) {}

    enum Status { RESERVED, FAILED, ALREADY_PROCESSED }

    record ReservationOutcome(Status status, String failureReason, List<ReservedItem> reservedItems) {
        public static ReservationOutcome reserved(List<ReservedItem> items) {
            return new ReservationOutcome(Status.RESERVED, null, items);
        }
        public static ReservationOutcome failed(String reason) {
            return new ReservationOutcome(Status.FAILED, reason, List.of());
        }
        public static ReservationOutcome alreadyProcessed() {
            return new ReservationOutcome(Status.ALREADY_PROCESSED, null, List.of());
        }
    }
}
