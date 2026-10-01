-- Phase 1 hardening: quest unique guard, session activity, ended_reason

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_quest_progress_user_quest
  ON user_quest_progress (user_id, quest_id);

ALTER TABLE visit_sessions ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ;
UPDATE visit_sessions SET last_activity_at = COALESCE(ended_at, started_at) WHERE last_activity_at IS NULL;

ALTER TABLE visit_sessions ADD COLUMN IF NOT EXISTS ended_reason VARCHAR(20);
