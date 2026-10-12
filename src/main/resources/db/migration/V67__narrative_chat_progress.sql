ALTER TABLE user_narrative_investigation_progress
    ADD COLUMN IF NOT EXISTS chat_node_codes_json TEXT NOT NULL DEFAULT '[]';
