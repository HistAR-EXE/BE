-- Keep V99 immutable for databases which already applied the original video schema.
-- New sessions populate the clock anchor; old sessions must reconnect before crediting progress.
ALTER TABLE explore_video_progress
    ADD COLUMN IF NOT EXISTS session_started_at timestamptz,
    ADD COLUMN IF NOT EXISTS session_started_seconds double precision NOT NULL DEFAULT 0;
