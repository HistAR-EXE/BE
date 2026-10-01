-- Backfill visit_sessions columns expected by current BE entity/runtime.

ALTER TABLE visit_sessions
    ADD COLUMN IF NOT EXISTS persona_goal VARCHAR(255),
    ADD COLUMN IF NOT EXISTS session_duration VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ai_tone VARCHAR(64);

