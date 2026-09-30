-- V35: Scope RAG retrieval by site so ST01 at Hue does not pull Cu Chi chunks.

ALTER TABLE rag_sources
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

ALTER TABLE rag_chunks
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

-- Existing Cu Chi corpus rows (station-tagged) default to cu-chi
UPDATE rag_sources SET site_code = 'cu-chi'
WHERE site_code IS NULL AND station_code IS NOT NULL;

UPDATE rag_chunks SET site_code = 'cu-chi'
WHERE site_code IS NULL AND station_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rag_sources_site_station ON rag_sources (site_code, station_code);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_site_station ON rag_chunks (site_code, station_code);
