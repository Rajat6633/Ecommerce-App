-- ============================================================================
--  product_db — initial schema (categories, products).
-- ============================================================================

CREATE TABLE categories (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    parent_id  UUID REFERENCES categories(id),
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE products (
    id          UUID PRIMARY KEY,
    sku         VARCHAR(64)  NOT NULL UNIQUE,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(12,2) NOT NULL,
    currency    VARCHAR(3)   NOT NULL,
    category_id UUID         NOT NULL REFERENCES categories(id),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_name     ON products(lower(name));
CREATE INDEX idx_products_active   ON products(active);
