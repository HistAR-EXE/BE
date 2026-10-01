-- FE compatibility migration pack (manual run, PostgreSQL)
-- Apply in non-prod first, then prod with backup.

BEGIN;

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS rating NUMERIC(2,1) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_ar_available BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS distance_km NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS sources TEXT,
    ADD COLUMN IF NOT EXISTS knowledge_context TEXT;

ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS steps_total INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS unlock_level INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS cover_image TEXT,
    ADD COLUMN IF NOT EXISTS status_default VARCHAR(30) NOT NULL DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

ALTER TABLE user_quest_progress
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS current_step INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS steps_total INT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token VARCHAR(1024) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMIT;
