-- ============================================================================
--  kyc_db — KYC cases + watchlist hits, the embedded watchlist (pgvector), the
--  document references, and the idempotency ledger. Forward-only (Flyway).
--  Requires the pgvector extension.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE kyc_cases (
    id              UUID PRIMARY KEY,
    user_id         UUID         NOT NULL,
    status          VARCHAR(16)  NOT NULL,
    risk_score      DOUBLE PRECISION,
    decision_reason VARCHAR(1024),
    resolved_by     VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL,
    updated_at      TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_kyc_cases_user_id UNIQUE (user_id)
);

CREATE INDEX idx_kyc_cases_status ON kyc_cases (status);

CREATE TABLE watchlist_hits (
    id           UUID PRIMARY KEY,
    case_id      UUID         NOT NULL REFERENCES kyc_cases (id) ON DELETE CASCADE,
    list_source  VARCHAR(64)  NOT NULL,
    matched_name VARCHAR(512) NOT NULL,
    score        DOUBLE PRECISION NOT NULL,
    payload      TEXT
);

CREATE INDEX idx_watchlist_hits_case_id ON watchlist_hits (case_id);

-- Ingested OFAC/UN/EU rows + their embedding vectors. 384 dims = the default
-- all-MiniLM-L6-v2 Transformers embedding model. A scheduled feed parser owns
-- this in production; local/dev seed a tiny fixture list (WatchlistSeeder).
CREATE TABLE watchlist_entries (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    list_source VARCHAR(64)  NOT NULL,
    entry_name  VARCHAR(512) NOT NULL,
    embedding   vector(384),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Spring AI's pgvector VectorStore manages its own table (initialize-schema is
-- false, so we create it here for Flyway to own). Reused for sanctions screening.
CREATE TABLE vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding vector(384)
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops);

-- Document references + extracted fields. Never the raw image bytes (PII);
-- redactable for GDPR erasure.
CREATE TABLE kyc_documents (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    case_id           UUID NOT NULL REFERENCES kyc_cases (id) ON DELETE CASCADE,
    document_ref      VARCHAR(512),
    extracted_name    VARCHAR(512),
    extracted_doc_no  VARCHAR(128),
    extracted_dob     VARCHAR(32),
    extracted_expiry  VARCHAR(32),
    nationality       VARCHAR(64),
    redacted          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_kyc_documents_case_id ON kyc_documents (case_id);

CREATE TABLE processed_events (
    event_id     UUID PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL
);
