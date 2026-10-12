ALTER TABLE user_narrative_investigation_progress
    ADD COLUMN IF NOT EXISTS observation_codes_json TEXT NOT NULL DEFAULT '[]';
