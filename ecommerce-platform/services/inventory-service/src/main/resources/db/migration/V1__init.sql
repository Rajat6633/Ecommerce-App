-- ============================================================================
--  inventory_db — stock, reservations, and the idempotency ledger.
-- ============================================================================

CREATE TABLE inventory_items (
    id            UUID PRIMARY KEY,
    product_id    UUID NOT NULL UNIQUE,
    on_hand       INT  NOT NULL DEFAULT 0,
    reserved      INT  NOT NULL DEFAULT 0,
    reorder_level INT  NOT NULL DEFAULT 0,
    version       BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE stock_reservations (
    id         UUID PRIMARY KEY,
    order_id   UUID        NOT NULL,
    product_id UUID        NOT NULL,
    quantity   INT         NOT NULL,
    status     VARCHAR(16) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reservations_order ON stock_reservations(order_id);

-- Consumer idempotency ledger (dedupe by event id).
CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
