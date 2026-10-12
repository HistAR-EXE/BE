-- Required by the pgvector RAG schema (V25/V58).
-- Safe for fresh databases; existing volumes are handled by docker-db-seed.sh.
CREATE EXTENSION IF NOT EXISTS vector;
