-- V88 performs idempotent artifact upserts by (location_id, unlock_key).
-- Make that business key explicit for clean databases that did not previously
-- receive Hibernate-generated constraints.
CREATE UNIQUE INDEX IF NOT EXISTS uq_artifacts_location_unlock_key
    ON artifacts(location_id, unlock_key);
