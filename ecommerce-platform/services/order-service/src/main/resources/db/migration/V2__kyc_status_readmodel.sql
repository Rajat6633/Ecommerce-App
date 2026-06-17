-- ============================================================================
--  order_db — local KYC read-model (Phase 14b). Projected from the kyc.approved
--  / kyc.rejected Kafka stream; read by the placeOrder checkout gate. No FK to
--  any other service's schema (database-per-service).
-- ============================================================================

CREATE TABLE customer_kyc_status (
    user_id    UUID PRIMARY KEY,
    status     VARCHAR(16) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
