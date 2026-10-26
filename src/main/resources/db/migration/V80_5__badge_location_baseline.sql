-- Badge.locationId historically came from Hibernate ddl-auto. V81 seeds
-- location-scoped badges, so clean Flyway-only databases need the column first.
ALTER TABLE badges
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_badges_location_id ON badges(location_id);
