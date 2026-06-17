-- ============================================================================
--  payment_db — payments (one per order) + the idempotency ledger.
-- ============================================================================

CREATE TABLE payments (
    id             UUID PRIMARY KEY,
    order_id       UUID         NOT NULL UNIQUE,
    amount         NUMERIC(12,2) NOT NULL,
    currency       VARCHAR(3)   NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL
);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
