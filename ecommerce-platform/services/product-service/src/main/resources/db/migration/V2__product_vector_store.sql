-- ============================================================================
--  product_db — semantic search (D1) + recommendations (D5) support.
--
--  Spring AI's pgvector VectorStore manages its own table. We run with
--  spring.ai.vectorstore.pgvector.initialize-schema=false (docker/k8s), so
--  Flyway owns the schema here. 384 dims = the default all-MiniLM-L6-v2
--  Transformers embedding model used under docker/k8s (local/test use the
--  in-memory SimpleVectorStore + stub model instead, so this table is unused
--  there). Requires the pgvector extension.
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE vector_store (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content   TEXT,
    metadata  JSONB,
    embedding vector(384)
);

CREATE INDEX idx_vector_store_embedding ON vector_store
    USING hnsw (embedding vector_cosine_ops);
