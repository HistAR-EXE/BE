-- Phase B upgrade: session ownership, analytics schema, multi-site keys, composite artifacts

-- B1: Visit session ownership
ALTER TABLE visit_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(16) DEFAULT 'ACTIVE';
UPDATE visit_sessions SET status = 'CLOSED' WHERE ended_at IS NOT NULL AND (status IS NULL OR status = 'ACTIVE');
UPDATE visit_sessions SET status = 'ACTIVE' WHERE ended_at IS NULL AND status IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_visit_sessions_one_active
  ON visit_sessions (user_id, location_id)
  WHERE status = 'ACTIVE' AND ended_at IS NULL;

-- B3: Analytics events schema
ALTER TABLE analytics_events ADD COLUMN IF NOT EXISTS content_type VARCHAR(32);

-- B8: Multi-site user_discoveries scope
ALTER TABLE user_discoveries ADD COLUMN IF NOT EXISTS location_id UUID;
UPDATE user_discoveries ud
SET location_id = dp.location_id
FROM discovery_points dp
WHERE dp.unlock_key = ud.discovery_key AND ud.location_id IS NULL;
UPDATE user_discoveries
SET location_id = '11111111-1111-1111-1111-111111111111'
WHERE location_id IS NULL;
ALTER TABLE user_discoveries ALTER COLUMN location_id SET NOT NULL;
ALTER TABLE user_discoveries DROP CONSTRAINT IF EXISTS user_discoveries_pkey;
ALTER TABLE user_discoveries ADD PRIMARY KEY (user_id, location_id, discovery_key);

-- B9: Composite artifact demo — Cuộc Sống Dưới Lòng Đất
INSERT INTO artifacts (id, location_id, name, image_url, description, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000099',
  '11111111-1111-1111-1111-111111111111',
  'Cuộc Sống Dưới Lòng Đất',
  '/media/cu-chi/scenes/phong-hop-2026.jpg',
  'Bộ sưu tập đặc biệt — khám phá Bếp Hoàng Cầm, Phòng họp và Trạm xá.',
  'artifact:cuoc-song-duoi-long-dat',
  'primary',
  99
) ON CONFLICT (id) DO NOTHING;

INSERT INTO artifact_unlock_requirements (location_id, artifact_unlock_key, required_discovery_key)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'artifact:cuoc-song-duoi-long-dat', 'scene:22222222-2222-2222-2222-222222222221'),
  ('11111111-1111-1111-1111-111111111111', 'artifact:cuoc-song-duoi-long-dat', 'scene:22222222-2222-2222-2222-222222222223'),
  ('11111111-1111-1111-1111-111111111111', 'artifact:cuoc-song-duoi-long-dat', 'artifact:tram-xa')
ON CONFLICT DO NOTHING;

-- B8: Discovery content bindings seed (Cu Chi)
INSERT INTO discovery_content_bindings (location_id, unlock_key, record_key, engagement, href_template, sort_order)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'scene:22222222-2222-2222-2222-222222222221', 'hotspot:kitchen', 'tour_panorama', '/tour360/{locationId}?scene={recordKey}', 1),
  ('11111111-1111-1111-1111-111111111111', 'scene:22222222-2222-2222-2222-222222222223', 'hotspot:meeting', 'tour_panorama', '/tour360/{locationId}?scene={recordKey}', 2),
  ('11111111-1111-1111-1111-111111111111', 'era:1948', 'era:1948', 'time_portal', '/time-portal/{locationId}?era=1948', 3)
ON CONFLICT (location_id, unlock_key) DO NOTHING;
