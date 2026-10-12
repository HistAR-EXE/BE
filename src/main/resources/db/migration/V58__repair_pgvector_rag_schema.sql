-- Repair legacy databases where V25 was recorded after rag_chunks / semantic_cache
-- already existed without their vector columns. Do not fail non-pgvector hosts:
-- RAG remains disabled there until the host is upgraded to pgvector.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;

        IF to_regclass('public.rag_chunks') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE public.rag_chunks ADD COLUMN IF NOT EXISTS embedding vector(768)';
        END IF;

        IF to_regclass('public.semantic_cache') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE public.semantic_cache ADD COLUMN IF NOT EXISTS embedding vector(768)';
        END IF;
    ELSE
        RAISE NOTICE 'pgvector is not available on this PostgreSQL host; RAG embeddings remain disabled.';
    END IF;
END $$;
