package com.ecommerce.common.events;

/** Canonical Kafka topic names shared across services (single source of truth). */
public final class Topics {

    private Topics() {
    }

    public static final String ORDER_CREATED = "order.created";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_RESERVATION_FAILED = "inventory.reservation-failed";
    public static final String INVENTORY_RELEASED = "inventory.released";
    public static final String PAYMENT_COMPLETED = "payment.completed";
    public static final String PAYMENT_FAILED = "payment.failed";
    public static final String ORDER_CONFIRMED = "order.confirmed";

    // KYC / compliance (Phase 14). user.registered is the trigger emitted by
    // auth-service; kyc.approved / kyc.rejected are produced by kyc-service.
    public static final String USER_REGISTERED = "user.registered";
    public static final String KYC_APPROVED = "kyc.approved";
    public static final String KYC_REJECTED = "kyc.rejected";
}
