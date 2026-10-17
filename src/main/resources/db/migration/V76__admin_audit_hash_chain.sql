-- Admin audit SHA-256 hash chain (prev + event hash).
-- Row hashes are backfilled by AdminAuditHashBackfill on startup so Instant formatting matches Java verify.

ALTER TABLE admin_audit_events
    ADD COLUMN IF NOT EXISTS event_hash VARCHAR(64);

ALTER TABLE admin_audit_events
    ADD COLUMN IF NOT EXISTS prev_event_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_admin_audit_events_occurred_id
    ON admin_audit_events (occurred_at ASC, id ASC);
