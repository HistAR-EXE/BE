-- Repair databases that were baselined or bootstrapped before the complete
-- admin audit/optimistic-lock schema from V61 was applied.
-- Every statement is idempotent so this migration is also safe on healthy DBs.

CREATE TABLE IF NOT EXISTS admin_audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(96) NOT NULL,
    old_row_version BIGINT,
    new_row_version BIGINT,
    reason TEXT,
    idempotency_key VARCHAR(160),
    metadata_json TEXT NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_events_entity_occurred
    ON admin_audit_events(entity_type, entity_id, occurred_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_audit_events_idempotency
    ON admin_audit_events(entity_type, entity_id, action, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS fact_verified BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_discovery_points_deleted_at ON discovery_points(deleted_at);
CREATE INDEX IF NOT EXISTS idx_artifacts_deleted_at ON artifacts(deleted_at);
CREATE INDEX IF NOT EXISTS idx_quests_deleted_at ON quests(deleted_at);
