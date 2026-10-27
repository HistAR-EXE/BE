ALTER TABLE user_creations
    ADD COLUMN IF NOT EXISTS kind varchar(16) NOT NULL DEFAULT 'original',
    ADD COLUMN IF NOT EXISTS site_code varchar(64),
    ADD COLUMN IF NOT EXISTS station_code varchar(16),
    ADD COLUMN IF NOT EXISTS role_id varchar(128),
    ADD COLUMN IF NOT EXISTS era smallint,
    ADD COLUMN IF NOT EXISTS source varchar(32);

CREATE INDEX IF NOT EXISTS idx_user_creations_station
    ON user_creations (station_code, created_at DESC);
