-- CP3 gamification upgrade (additive): photo_scenes, time_layers, artifacts,
-- user_artifacts, discovery_points, user_discoveries, hotspot_contents
-- Location template: Củ Chi 11111111-1111-1111-1111-111111111111

-- ---------- photo_scenes + time_layers (A2) ----------
CREATE TABLE IF NOT EXISTS photo_scenes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS time_layers (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    scene_id   UUID NOT NULL REFERENCES photo_scenes(id) ON DELETE CASCADE,
    era        INT NOT NULL,
    image_url  TEXT,
    caption    TEXT,
    UNIQUE (scene_id, era)
);

CREATE INDEX IF NOT EXISTS idx_photo_scenes_location ON photo_scenes(location_id);
CREATE INDEX IF NOT EXISTS idx_time_layers_scene ON time_layers(scene_id);

-- ---------- artifacts + user_artifacts (C1) ----------
CREATE TABLE IF NOT EXISTS artifacts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    image_url   TEXT,
    description TEXT,
    unlock_key  VARCHAR(128) NOT NULL,
    reliability VARCHAR(32) DEFAULT 'primary',
    sort_order  INT NOT NULL DEFAULT 0,
    UNIQUE (location_id, unlock_key)
);

CREATE TABLE IF NOT EXISTS user_artifacts (
    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    artifact_id UUID NOT NULL REFERENCES artifacts(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, artifact_id)
);

CREATE INDEX IF NOT EXISTS idx_artifacts_location ON artifacts(location_id);
CREATE INDEX IF NOT EXISTS idx_user_artifacts_user ON user_artifacts(user_id);

-- ---------- discovery_points + user_discoveries (B1/B3) ----------
CREATE TABLE IF NOT EXISTS discovery_points (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    map_x_pct   NUMERIC(5,2) NOT NULL DEFAULT 50,
    map_y_pct   NUMERIC(5,2) NOT NULL DEFAULT 50,
    unlock_key  VARCHAR(128) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    UNIQUE (location_id, unlock_key)
);

CREATE TABLE IF NOT EXISTS user_discoveries (
    user_id        UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    discovery_key  VARCHAR(128) NOT NULL,
    discovered_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, discovery_key)
);

CREATE INDEX IF NOT EXISTS idx_discovery_points_location ON discovery_points(location_id);
CREATE INDEX IF NOT EXISTS idx_user_discoveries_user ON user_discoveries(user_id);

-- ---------- hotspot_contents (B2) ----------
CREATE TABLE IF NOT EXISTS hotspot_contents (
    content_ref VARCHAR(128) PRIMARY KEY,
    title       VARCHAR(255),
    description TEXT,
    image_url   TEXT,
    unlock_key  VARCHAR(128)
);

-- ---------- seed: photo_scenes (5 scenes × 3 eras) ----------
INSERT INTO photo_scenes (id, location_id, name, sort_order) VALUES
('50000001-0000-4000-8000-000000000001', '11111111-1111-1111-1111-111111111111', 'Cửa hầm địa đạo', 1),
('50000001-0000-4000-8000-000000000002', '11111111-1111-1111-1111-111111111111', 'Bếp Hoàng Cầm', 2),
('50000001-0000-4000-8000-000000000003', '11111111-1111-1111-1111-111111111111', 'Phòng họp dưới lòng đất', 3),
('50000001-0000-4000-8000-000000000004', '11111111-1111-1111-1111-111111111111', 'Hệ thống thông gió', 4),
('50000001-0000-4000-8000-000000000005', '11111111-1111-1111-1111-111111111111', 'Giếng nước trong địa đạo', 5)
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name, sort_order = EXCLUDED.sort_order;

INSERT INTO time_layers (scene_id, era, image_url, caption) VALUES
-- Cửa hầm
('50000001-0000-4000-8000-000000000001', 1948, '/media/cu-chi/map/hero.jpg', '1948 — dân làng Tân Phú Trung, Phước Vĩnh An bắt đầu đào hầm bí mật'),
('50000001-0000-4000-8000-000000000001', 1968, '/media/cu-chi/scenes/cua-ham-1968.png', '1968 — Tết Mậu Thân, bàn đạp tiến vào Sài Gòn'),
('50000001-0000-4000-8000-000000000001', 2026, '/media/cu-chi/scenes/cua-ham-2026.jpg', '2026 — Khu di tích lịch sử Địa đạo Củ Chi hôm nay'),
-- Bếp Hoàng Cầm
('50000001-0000-4000-8000-000000000002', 1948, '/media/cu-chi/map/hero.jpg', '1948 — khởi đầu hệ thống hầm ngầm tại Củ Chi'),
('50000001-0000-4000-8000-000000000002', 1968, '/media/cu-chi/scenes/bep-hoang-cam-1968.png', '1968 — Bếp Hoàng Cầm nuôi dưỡng chiến sĩ dưới lòng đất'),
('50000001-0000-4000-8000-000000000002', 2026, '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', '2026 — Mô hình bếp Hoàng Cầm tại khu di tích'),
-- Phòng họp
('50000001-0000-4000-8000-000000000003', 1948, '/media/cu-chi/map/hero.jpg', '1948 — ý chí đào hầm lan rộng trong dân làng'),
('50000001-0000-4000-8000-000000000003', 1968, '/media/cu-chi/scenes/phong-hop-1968.png', '1968 — Phòng họp bí mật dưới lòng đất'),
('50000001-0000-4000-8000-000000000003', 2026, '/media/cu-chi/scenes/phong-hop-2026.jpg', '2026 — Không gian trưng bày phòng họp'),
-- Thông gió
('50000001-0000-4000-8000-000000000004', 1948, '/media/cu-chi/map/hero.jpg', '1948 — hầm đầu tiên chưa có hệ thống thông gió hoàn chỉnh'),
('50000001-0000-4000-8000-000000000004', 1968, '/media/cu-chi/scenes/thong-gio-1968.png', '1968 — Lỗ thông hơi ngụy trang giữ bí mật địa đạo'),
('50000001-0000-4000-8000-000000000004', 2026, '/media/cu-chi/scenes/thong-gio-2026.jpg', '2026 — Hệ thống thông gió hiện trạng'),
-- Giếng nước
('50000001-0000-4000-8000-000000000005', 1948, '/media/cu-chi/map/hero.jpg', '1948 — giếng nước là nguồn sống của hầm ngầm'),
('50000001-0000-4000-8000-000000000005', 1968, '/media/cu-chi/scenes/gieng-1968.png', '1968 — Giếng nước ngầm phục vụ chiến sĩ'),
('50000001-0000-4000-8000-000000000005', 2026, '/media/cu-chi/scenes/gieng-2026.jpg', '2026 — Giếng nước tại khu di tích')
ON CONFLICT (scene_id, era) DO UPDATE
SET image_url = EXCLUDED.image_url, caption = EXCLUDED.caption;

-- ---------- seed: artifacts (15 + bonus) ----------
INSERT INTO artifacts (id, location_id, name, image_url, description, unlock_key, reliability, sort_order) VALUES
('a0000001-0000-4000-8000-000000000001', '11111111-1111-1111-1111-111111111111', 'Bếp Hoàng Cầm (mô hình)', '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', 'Bếp Hoàng Cầm — nơi nấu ăn bí mật dưới lòng đất, không khói, không mùi.', 'hotspot:kitchen', 'primary', 1),
('a0000001-0000-4000-8000-000000000002', '11111111-1111-1111-1111-111111111111', 'Cuốc chim', '/media/cu-chi/map/hero.jpg', 'Dụng cụ đào hầm đặc trưng — cuốc chim một lưỡi.', 'artifact:cuoc-chim', 'primary', 2),
('a0000001-0000-4000-8000-000000000003', '11111111-1111-1111-1111-111111111111', 'Chông tre / bẫy chông', '/media/cu-chi/map/hero.jpg', 'Vật liệu phòng thủ quanh khu vực địa đạo.', 'artifact:chong-tre', 'primary', 3),
('a0000001-0000-4000-8000-000000000004', '11111111-1111-1111-1111-111111111111', 'Mìn gạt chống tăng', '/media/cu-chi/map/hero.jpg', 'Vũ khí tự chế chống xe bọc thép.', 'artifact:min-gat', 'primary', 4),
('a0000001-0000-4000-8000-000000000005', '11111111-1111-1111-1111-111111111111', 'Nắp hầm bí mật', '/media/cu-chi/scenes/cua-ham-1968.png', 'Nắp hầm ngụy trang bằng lá, đất và cỏ.', 'artifact:nap-ham', 'primary', 5),
('a0000001-0000-4000-8000-000000000006', '11111111-1111-1111-1111-111111111111', 'Lỗ thông hơi ngụy trang', '/media/cu-chi/scenes/thong-gio-2026.jpg', 'Lỗ thông hơi được che kín, khó phát hiện từ trên mặt đất.', 'hotspot:vent', 'primary', 6),
('a0000001-0000-4000-8000-000000000007', '11111111-1111-1111-1111-111111111111', 'Giếng nước ngầm', '/media/cu-chi/scenes/gieng-2026.jpg', 'Giếng nước sâu trong địa đạo — nguồn sống của chiến sĩ.', 'scene:gieng', 'primary', 7),
('a0000001-0000-4000-8000-000000000008', '11111111-1111-1111-1111-111111111111', 'Lao tre', '/media/cu-chi/map/hero.jpg', 'Lao tre chống địch từ trên xuống.', 'artifact:lao-tre', 'primary', 8),
('a0000001-0000-4000-8000-000000000009', '11111111-1111-1111-1111-111111111111', 'Đèn dầu hầm phẫu thuật', '/media/cu-chi/map/hero.jpg', 'Đèn dầu chiếu sáng trong hầm y tế ngầm.', 'artifact:den-dau', 'primary', 9),
('a0000001-0000-4000-8000-000000000010', '11111111-1111-1111-1111-111111111111', 'Khăn rằn Nam Bộ', '/media/cu-chi/map/hero.jpg', 'Trang phục dân tộc gắn với du kích Nam Bộ.', 'artifact:khan-ran', 'primary', 10),
('a0000001-0000-4000-8000-000000000011', '11111111-1111-1111-1111-111111111111', 'Áo bà ba', '/media/cu-chi/map/hero.jpg', 'Trang phục dân gian — theo tư liệu tham khảo.', 'artifact:ao-ba-ba', 'secondary', 11),
('a0000001-0000-4000-8000-000000000012', '11111111-1111-1111-1111-111111111111', 'Mũ tai bèo', '/media/cu-chi/map/hero.jpg', 'Mũ tai bèo — theo tư liệu tham khảo.', 'artifact:mu-tai-beo', 'secondary', 12),
('a0000001-0000-4000-8000-000000000013', '11111111-1111-1111-1111-111111111111', 'Dép lốp cao su', '/media/cu-chi/map/hero.jpg', 'Dép lốp — theo tư liệu tham khảo.', 'artifact:dep-lop', 'secondary', 13),
('a0000001-0000-4000-8000-000000000014', '11111111-1111-1111-1111-111111111111', 'Báo Giải Phóng', '/media/cu-chi/map/hero.jpg', 'Tờ báo thời kháng chiến truyền tin tức chiến trường.', 'artifact:bao-giai-phong', 'primary', 14),
('a0000001-0000-4000-8000-000000000015', '11111111-1111-1111-1111-111111111111', 'Cưa bom / TNT tự chế', '/media/cu-chi/map/hero.jpg', 'Vũ khí tự chế từ vật liệu thu được.', 'artifact:cua-bom', 'primary', 15),
('a0000001-0000-4000-8000-000000000016', '11111111-1111-1111-1111-111111111111', 'Súng DKZ', '/media/cu-chi/artifacts/trung-bay-vu-khi.png', 'Súng DKZ trưng bày tại Bến Dược.', 'artifact:sung-dkz', 'primary', 16),
('a0000001-0000-4000-8000-000000000017', '11111111-1111-1111-1111-111111111111', 'Pháo 105 / Pháo 175', '/media/cu-chi/artifacts/trung-bay-vu-khi.png', 'Pháo trưng bày tại khu di tích.', 'artifact:phao-105', 'primary', 17)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
    reliability = EXCLUDED.reliability, sort_order = EXCLUDED.sort_order;

-- ---------- seed: discovery_points (12 POI) ----------
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000001', '11111111-1111-1111-1111-111111111111', 'Cổng vào', 48.00, 72.00, 'scene:22222222-2222-2222-2222-222222222222', 1),
('d0000001-0000-4000-8000-000000000002', '11111111-1111-1111-1111-111111111111', 'Bếp Hoàng Cầm', 35.00, 45.00, 'scene:22222222-2222-2222-2222-222222222221', 2),
('d0000001-0000-4000-8000-000000000003', '11111111-1111-1111-1111-111111111111', 'Phòng họp', 62.00, 40.00, 'scene:22222222-2222-2222-2222-222222222223', 3),
('d0000001-0000-4000-8000-000000000004', '11111111-1111-1111-1111-111111111111', 'Cửa hầm', 52.00, 58.00, 'photo:cua-ham', 4),
('d0000001-0000-4000-8000-000000000005', '11111111-1111-1111-1111-111111111111', 'Giếng nước', 70.00, 55.00, 'photo:gieng', 5),
('d0000001-0000-4000-8000-000000000006', '11111111-1111-1111-1111-111111111111', 'Hầm chế tạo vũ khí', 28.00, 62.00, 'artifact:vu-khi', 6),
('d0000001-0000-4000-8000-000000000007', '11111111-1111-1111-1111-111111111111', 'Trạm xá', 42.00, 68.00, 'artifact:tram-xa', 7),
('d0000001-0000-4000-8000-000000000008', '11111111-1111-1111-1111-111111111111', 'Lỗ thông hơi', 58.00, 32.00, 'hotspot:vent', 8),
('d0000001-0000-4000-8000-000000000009', '11111111-1111-1111-1111-111111111111', 'Bẫy du kích', 22.00, 38.00, 'artifact:bay', 9),
('d0000001-0000-4000-8000-000000000010', '11111111-1111-1111-1111-111111111111', '1948 — khởi đầu hầm ngầm', 50.00, 18.00, 'era:1948', 10),
('d0000001-0000-4000-8000-000000000011', '11111111-1111-1111-1111-111111111111', 'Tết Mậu Thân 1968', 45.00, 25.00, 'era:1968', 11),
('d0000001-0000-4000-8000-000000000012', '11111111-1111-1111-1111-111111111111', 'Khu di tích 2026', 55.00, 82.00, 'era:2026', 12)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name, map_x_pct = EXCLUDED.map_x_pct, map_y_pct = EXCLUDED.map_y_pct, sort_order = EXCLUDED.sort_order;

-- ---------- seed: hotspot_contents (B2) ----------
INSERT INTO hotspot_contents (content_ref, title, description, image_url, unlock_key) VALUES
('tunnel-entrance', 'Cửa hầm chính', 'Cửa hầm được ngụy trang kỹ lưỡng — điểm vào bí mật của hệ thống địa đạo.', '/media/cu-chi/scenes/cua-ham-2026.jpg', 'photo:cua-ham'),
('kitchen', 'Bếp Hoàng Cầm', 'Bếp Hoàng Cầm nấu ăn không khói, không mùi — nuôi dưỡng chiến sĩ trong lòng đất.', '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', 'hotspot:kitchen'),
('meeting-room', 'Phòng họp', 'Phòng họp dưới lòng đất — nơi bàn bạc chiến lược và chỉ đạo.', '/media/cu-chi/scenes/phong-hop-2026.jpg', 'scene:22222222-2222-2222-2222-222222222223'),
('vent-hole', 'Lỗ thông hơi', 'Lỗ thông hơi ngụy trang giúp không khí lưu thông mà không bị lộ.', '/media/cu-chi/scenes/thong-gio-2026.jpg', 'hotspot:vent')
ON CONFLICT (content_ref) DO UPDATE
SET title = EXCLUDED.title, description = EXCLUDED.description, image_url = EXCLUDED.image_url, unlock_key = EXCLUDED.unlock_key;

-- Info hotspots for Bến Dược scenes (if not present)
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222221', 0.8, 0.0, 'info', 'kitchen', 'Bếp Hoàng Cầm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222221' AND content_ref = 'kitchen'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222223', -0.5, 0.1, 'info', 'meeting-room', 'Phòng họp'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222223' AND content_ref = 'meeting-room'
);
