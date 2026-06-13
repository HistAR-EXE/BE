-- Unlock rules engine (Phase B) — additive; legacy path remains default when flag off
BEGIN;

CREATE TABLE IF NOT EXISTS unlock_rules (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id  UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    trigger_type VARCHAR(50) NOT NULL,
    trigger_key  VARCHAR(128) NOT NULL,
    reward_type  VARCHAR(50) NOT NULL,
    reward_key   VARCHAR(128) NOT NULL,
    enabled      BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order   INT NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (location_id, trigger_type, trigger_key, reward_type, reward_key)
);

-- Mirror legacy DiscoveryServiceImpl.recordOnCheckin + ArtifactServiceImpl.CHECKIN_UNLOCK_KEYS
INSERT INTO unlock_rules (location_id, trigger_type, trigger_key, reward_type, reward_key, sort_order) VALUES
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'discovery', 'era:2026', 2),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'discovery', 'photo:cua-ham', 3),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'discovery', 'photo:gieng', 4),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'artifact', 'artifact:cuoc-chim', 10),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'artifact', 'artifact:chong-tre', 11),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'artifact', 'artifact:nap-ham', 12),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'artifact', 'artifact:den-dau', 13),
('11111111-1111-1111-1111-111111111111', 'checkin', 'location', 'artifact', 'artifact:khan-ran', 14)
ON CONFLICT (location_id, trigger_type, trigger_key, reward_type, reward_key) DO NOTHING;

COMMIT;
