-- DiscoveryContentBinding used to be created by Hibernate. V84 updates and
-- populates it, therefore a clean Flyway-only database needs the base table.
CREATE TABLE IF NOT EXISTS discovery_content_bindings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    unlock_key VARCHAR(255) NOT NULL,
    record_key VARCHAR(255),
    engagement VARCHAR(64),
    href_template TEXT,
    sort_order INTEGER DEFAULT 0,
    artifact_id UUID REFERENCES artifacts(id) ON DELETE SET NULL,
    xp_bonus INTEGER DEFAULT 0,
    quest_step_id UUID
);

CREATE INDEX IF NOT EXISTS idx_discovery_content_bindings_location
    ON discovery_content_bindings(location_id, sort_order);
CREATE UNIQUE INDEX IF NOT EXISTS uq_discovery_content_bindings_location_key
    ON discovery_content_bindings(location_id, unlock_key);
