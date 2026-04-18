-- Enable pgvector extension (requires Neon.tech Postgres or self-hosted Postgres with pgvector)
-- Seeding embeddings from Phase 1 means months of data will be ready by the time Phase 4 AI is built.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE entity_embeddings (
    id               BIGSERIAL PRIMARY KEY,
    firebase_uid     VARCHAR(255) NOT NULL,
    entity_type      VARCHAR(100) NOT NULL,
    entity_server_id UUID         NOT NULL,
    embedding        vector(1536),
    content          TEXT         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (firebase_uid, entity_type, entity_server_id)
);

CREATE INDEX idx_entity_embeddings_lookup  ON entity_embeddings (firebase_uid, entity_type);
CREATE INDEX idx_entity_embeddings_hnsw    ON entity_embeddings USING hnsw (embedding vector_cosine_ops);
