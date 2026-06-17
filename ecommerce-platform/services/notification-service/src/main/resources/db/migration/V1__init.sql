-- ============================================================================
--  notification_db — notification audit log + the idempotency ledger.
--  One notification per (reference_id, type); reference_id is the order id.
-- ============================================================================

CREATE TABLE notifications (
    id             UUID PRIMARY KEY,
    reference_id   UUID         NOT NULL,
    channel        VARCHAR(16)  NOT NULL,
    recipient      VARCHAR(255) NOT NULL,
    type           VARCHAR(32)  NOT NULL,
    status         VARCHAR(16)  NOT NULL,
    payload        TEXT         NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL,
    sent_at        TIMESTAMPTZ,
    CONSTRAINT uq_notifications_reference_type UNIQUE (reference_id, type)
);

CREATE INDEX idx_notifications_reference_id ON notifications (reference_id);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
