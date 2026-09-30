-- A2 QR-first presence: store how/where presence was proven and an idempotency key.

ALTER TABLE checkins
    ADD COLUMN IF NOT EXISTS station_code    VARCHAR(64),
    ADD COLUMN IF NOT EXISTS presence_score  INT,
    ADD COLUMN IF NOT EXISTS presence_method VARCHAR(16),
    ADD COLUMN IF NOT EXISTS client_uuid     UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_checkins_user_client_uuid
    ON checkins (user_id, client_uuid)
    WHERE client_uuid IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_checkins_user_station
    ON checkins (user_id, created_at DESC)
    WHERE station_code IS NOT NULL;
