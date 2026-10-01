-- Bridge discovery unlock_key → artifact unlock_key (keys that differ)
BEGIN;

CREATE TABLE IF NOT EXISTS discovery_artifact_links (
    discovery_unlock_key VARCHAR(128) NOT NULL,
    artifact_unlock_key  VARCHAR(128) NOT NULL,
    location_id          UUID REFERENCES locations(id) ON DELETE CASCADE,
    PRIMARY KEY (discovery_unlock_key, artifact_unlock_key)
);

INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('scene:22222222-2222-2222-2222-222222222221', 'hotspot:kitchen', '11111111-1111-1111-1111-111111111111'),
('hotspot:vent', 'hotspot:vent', '11111111-1111-1111-1111-111111111111'),
('photo:gieng', 'scene:gieng', '11111111-1111-1111-1111-111111111111'),
('photo:cua-ham', 'artifact:cuoc-chim', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

COMMIT;
