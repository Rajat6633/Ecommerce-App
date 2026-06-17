-- ============================================================================
--  cart_db — one cart per user, items as an element collection.
-- ============================================================================

CREATE TABLE carts (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE cart_items (
    cart_id    UUID          NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id UUID          NOT NULL,
    quantity   INT           NOT NULL,
    unit_price NUMERIC(12,2) NOT NULL,
    added_at   TIMESTAMPTZ   NOT NULL
);

CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
