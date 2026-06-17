-- ============================================================================
--  order_db — orders, items, status history, and the idempotency ledger.
-- ============================================================================

CREATE TABLE orders (
    id           UUID PRIMARY KEY,
    user_id      UUID         NOT NULL,
    status       VARCHAR(24)  NOT NULL,
    total_amount NUMERIC(12,2) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    version      BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_orders_user ON orders(user_id, created_at DESC);

CREATE TABLE order_items (
    id         UUID PRIMARY KEY,
    order_id   UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID          NOT NULL,
    quantity   INT           NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL
);
CREATE INDEX idx_order_items_order ON order_items(order_id);

CREATE TABLE order_status_history (
    id         UUID PRIMARY KEY,
    order_id   UUID         NOT NULL,
    status     VARCHAR(24)  NOT NULL,
    note       VARCHAR(255),
    changed_at TIMESTAMPTZ  NOT NULL
);
CREATE INDEX idx_history_order ON order_status_history(order_id);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
