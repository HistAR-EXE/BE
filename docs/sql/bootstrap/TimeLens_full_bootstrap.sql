-- HistAR full local bootstrap SQL
-- Generated from schema, seed manifest, and Flyway migrations through V90.
-- Run only on a new, empty local database with psql -v ON_ERROR_STOP=1 -f.
-- Do not run against shared, staging, or production databases.
\set ON_ERROR_STOP on
SET client_encoding TO 'UTF8';

-- ============================================================================
-- BEGIN schema: TimeLens_DB_Schema.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\schema\TimeLens_DB_Schema.sql
-- ============================================================================
-- ============================================================
-- TimeLens — PostgreSQL Schema (17 bảng) + Seed data Củ Chi
-- Chạy trên Supabase SQL Editor / Railway / Render Postgres
-- ============================================================

CREATE EXTENSION IF NOT EXISTS pgcrypto;  -- cho gen_random_uuid()

-- ---------- 1. profiles (user + auth + profile) ----------
CREATE TABLE profiles (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255),                 -- null nếu đăng nhập OAuth
    provider      VARCHAR(50)  DEFAULT 'local', -- local / google
    role          VARCHAR(50)  DEFAULT 'USER',
    tier          VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    display_name  VARCHAR(100),
    avatar_url    TEXT,
    level         INT DEFAULT 1,
    total_points  INT DEFAULT 0,
    city          VARCHAR(100),
    created_at    TIMESTAMPTZ DEFAULT now()
);

-- ---------- 2. locations ----------
CREATE TABLE locations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    city        VARCHAR(100),
    cover_image TEXT,
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ---------- 3. characters ----------
CREATE TABLE characters (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id    UUID REFERENCES locations(id) ON DELETE CASCADE,
    name           VARCHAR(150) NOT NULL,
    era            VARCHAR(100),
    persona_prompt TEXT,
    portrait_url   TEXT
);

-- ---------- 4. photo_pairs (Then vs Now Slider) ----------
CREATE TABLE photo_pairs (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id      UUID REFERENCES locations(id) ON DELETE CASCADE,
    historical_image TEXT,
    current_image    TEXT,
    year             INT,
    caption          VARCHAR(255),
    sort_order       INT DEFAULT 0
);

-- ---------- 5. panoramas (360° scenes) ----------
CREATE TABLE panoramas (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    image_url   TEXT,
    title       VARCHAR(200)
);

-- ---------- 6. hotspots (trong 360°) ----------
CREATE TABLE hotspots (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    panorama_id UUID REFERENCES panoramas(id) ON DELETE CASCADE,
    yaw         DOUBLE PRECISION,
    pitch       DOUBLE PRECISION,
    type        VARCHAR(50),     -- info / character / quest / scene
    content_ref VARCHAR(255),
    label       VARCHAR(150)
);

-- ---------- 7. quests ----------
CREATE TABLE quests (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id    UUID REFERENCES locations(id) ON DELETE CASCADE,
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    story          TEXT,
    points_reward  INT DEFAULT 0,
    required_order INT DEFAULT 0
);

-- ---------- 8. badges ----------
CREATE TABLE badges (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    icon_url        TEXT,
    condition_type  VARCHAR(50),   -- quest_complete / points / checkin
    condition_value INT
);

-- ---------- 9. photo_frames ----------
CREATE TABLE photo_frames (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(150),
    image_url  TEXT,
    era        VARCHAR(100),
    sort_order INT DEFAULT 0
);

-- ---------- 10. campaigns (seasonal — độc lập) ----------
CREATE TABLE campaigns (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(200),
    start_date   TIMESTAMPTZ,
    end_date     TIMESTAMPTZ,
    bonus_points INT DEFAULT 0
);

-- ---------- 11. user_quest_progress ----------
CREATE TABLE user_quest_progress (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES profiles(id) ON DELETE CASCADE,
    quest_id     UUID REFERENCES quests(id)   ON DELETE CASCADE,
    status       VARCHAR(30) DEFAULT 'not_started', -- not_started / in_progress / completed
    started_at   TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    UNIQUE (user_id, quest_id)
);

-- ---------- 12. user_badges ----------
CREATE TABLE user_badges (
    id        UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id   UUID REFERENCES profiles(id) ON DELETE CASCADE,
    badge_id  UUID REFERENCES badges(id)   ON DELETE CASCADE,
    earned_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE (user_id, badge_id)             -- chống double-award
);

-- ---------- 13. conversations ----------
CREATE TABLE conversations (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID REFERENCES profiles(id)   ON DELETE CASCADE,
    character_id UUID REFERENCES characters(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ DEFAULT now()
);

-- ---------- 14. messages ----------
CREATE TABLE messages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID REFERENCES conversations(id) ON DELETE CASCADE,
    role            VARCHAR(20),   -- user / assistant
    content         TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);

-- ---------- 15. checkins (QR + GPS) ----------
CREATE TABLE checkins (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID REFERENCES profiles(id)  ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    latitude    DOUBLE PRECISION,
    longitude   DOUBLE PRECISION,
    created_at  TIMESTAMPTZ DEFAULT now()
);

-- ---------- 16. user_creations (Photo Frame outputs) ----------
CREATE TABLE user_creations (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID REFERENCES profiles(id)     ON DELETE CASCADE,
    frame_id   UUID REFERENCES photo_frames(id) ON DELETE SET NULL,
    output_url TEXT,
    variant    VARCHAR(20) DEFAULT 'square',
    shared_at  TIMESTAMPTZ,
    created_at TIMESTAMPTZ DEFAULT now()
);

-- ---------- 17. user_secret_unlocks (gamification week 2) ----------
CREATE TABLE user_secret_unlocks (
    user_id     UUID REFERENCES profiles(id)  ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMPTZ DEFAULT now(),
    PRIMARY KEY (user_id, location_id)
);

-- ============================================================
-- INDEXES (cho query nhanh)
-- ============================================================
CREATE INDEX idx_characters_location   ON characters(location_id);
CREATE INDEX idx_photopairs_location   ON photo_pairs(location_id);
CREATE INDEX idx_panoramas_location    ON panoramas(location_id);
CREATE INDEX idx_hotspots_panorama     ON hotspots(panorama_id);
CREATE INDEX idx_quests_location       ON quests(location_id);
CREATE INDEX idx_uqp_user              ON user_quest_progress(user_id);
CREATE INDEX idx_userbadges_user       ON user_badges(user_id);
CREATE INDEX idx_conversations_user    ON conversations(user_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_checkins_user         ON checkins(user_id);
CREATE INDEX idx_checkins_location     ON checkins(location_id);
CREATE INDEX idx_profiles_points       ON profiles(total_points DESC); -- leaderboard
CREATE INDEX idx_profiles_city_points  ON profiles(city, total_points DESC);
CREATE INDEX idx_user_creations_user   ON user_creations(user_id, created_at DESC);
CREATE INDEX idx_secret_unlocks_user   ON user_secret_unlocks(user_id);

-- ============================================================
-- SEED DATA — Địa đạo Củ Chi (pilot)
-- ============================================================
-- 1 location
INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES (
  '11111111-1111-1111-1111-111111111111',
  'Địa đạo Củ Chi',
  'Hệ thống địa đạo lịch sử thời kháng chiến tại huyện Củ Chi, TP.HCM.',
  11.141591, 106.4615963, 'TP.HCM',
  '/media/cu-chi/map/hero.jpg'
);

-- 2 characters (persona_prompt dùng cho AI Chat)
INSERT INTO characters (location_id, name, era, persona_prompt, portrait_url) VALUES
(
  '11111111-1111-1111-1111-111111111111',
  'Chị Năm — Nữ du kích Củ Chi', '1965-1975',
  'Bạn là Chị Năm, một nữ du kích từng sống và chiến đấu tại địa đạo Củ Chi. Nói chuyện ấm áp, mộc mạc, giọng Nam Bộ. Kể chuyện đời sống trong địa đạo dựa trên sự thật lịch sử. QUAN TRỌNG: nếu không chắc về một sự kiện, hãy nói thật là không rõ, TUYỆT ĐỐI không bịa.',
  'https://placehold.co/800x800?text=Chi+Nam'
),
(
  '11111111-1111-1111-1111-111111111111',
  'Hướng dẫn viên lịch sử', 'Hiện đại',
  'Bạn là hướng dẫn viên lịch sử tại Củ Chi, kiến thức sâu, trình bày rõ ràng, thân thiện với người trẻ. Chỉ trả lời dựa trên dữ kiện lịch sử có thật; nếu không chắc, nói không rõ thay vì bịa.',
  'https://placehold.co/800x800?text=HDV'
);

-- 5 photo_pairs (placeholder)
INSERT INTO photo_pairs (location_id, historical_image, current_image, year, caption, sort_order) VALUES
('11111111-1111-1111-1111-111111111111','https://placehold.co/800x600?text=1968','https://placehold.co/800x600?text=Now',1968,'Cửa hầm địa đạo',1),
('11111111-1111-1111-1111-111111111111','https://placehold.co/800x600?text=1968b','https://placehold.co/800x600?text=Nowb',1968,'Bếp Hoàng Cầm',2),
('11111111-1111-1111-1111-111111111111','https://placehold.co/800x600?text=1970','https://placehold.co/800x600?text=Nowc',1970,'Phòng họp dưới lòng đất',3),
('11111111-1111-1111-1111-111111111111','https://placehold.co/800x600?text=1972','https://placehold.co/800x600?text=Nowd',1972,'Hệ thống thông gió',4),
('11111111-1111-1111-1111-111111111111','https://placehold.co/800x600?text=1975','https://placehold.co/800x600?text=Nowe',1975,'Giếng nước trong địa đạo',5);

-- 1 quest (fixed UUID for FE/docs)
INSERT INTO quests (id, location_id, title, description, story, points_reward, required_order)
VALUES (
  '33333333-3333-3333-3333-333333333333',
  '11111111-1111-1111-1111-111111111111',
  'Hành trình dưới lòng đất',
  'Khám phá địa đạo và check-in tại Củ Chi.',
  'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Câu chuyện bí mật chỉ mở khi bạn hoàn thành hành trình và quét mã QR ẩn.',
  100, 1
);

-- 3 badges
INSERT INTO badges (id, name, description, icon_url, condition_type, condition_value) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'Người khám phá', 'Hoàn thành nhiệm vụ đầu tiên', 'https://placehold.co/200?text=Badge1', 'quest_complete', 1),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'Nhà sử học nhí', 'Đạt 300 điểm', 'https://placehold.co/200?text=Badge2', 'points', 300),
('cccccccc-cccc-cccc-cccc-cccccccccccc', 'Lần đầu check-in', 'Check-in tại di tích lần đầu', 'https://placehold.co/200?text=Badge3', 'checkin', 1);

-- 3 photo_frames
INSERT INTO photo_frames (name, image_url, era, sort_order) VALUES
('Khung du kích', 'https://placehold.co/1080?text=Frame1', '1968', 1),
('Khung áo bà ba', 'https://placehold.co/1080?text=Frame2', 'Cổ điển', 2),
('Khung vintage Củ Chi', 'https://placehold.co/1080?text=Frame3', 'Vintage', 3);

-- 1 panorama (360 tour pilot)
INSERT INTO panoramas (id, location_id, image_url, title)
VALUES (
  '22222222-2222-2222-2222-222222222222',
  '11111111-1111-1111-1111-111111111111',
  'https://placehold.co/4096x2048?text=Cu+Chi+360',
  'Địa đạo Củ Chi — góc nhìn 360°'
);

-- 3 hotspots
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label) VALUES
('22222222-2222-2222-2222-222222222222', 0.5, 0.1, 'info', 'tunnel-entrance', 'Cửa hầm chính'),
('22222222-2222-2222-2222-222222222222', -1.2, 0.0, 'info', 'kitchen', 'Bếp Hoàng Cầm'),
('22222222-2222-2222-2222-222222222222', 2.0, -0.2, 'scene', 'meeting-room', 'Phòng họp dưới lòng đất');

-- ============================================================
-- MINIMAL SEED — leaderboard & chat demo (Cu Chi only)
-- ============================================================

-- Demo profiles for leaderboard
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
SELECT
    'user' || i || '@histar.vn',
    '$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe',
    'local',
    'USER',
    'Khám phá viên ' || i,
    NULL,
    1 + ((i - 1) % 4),
    40 * i,
    'TP.HCM',
    now() - ((13 - i) || ' days')::interval
FROM generate_series(1, 8) AS g(i)
ON CONFLICT (email) DO NOTHING;

-- Conversations with Chị Năm
INSERT INTO conversations (user_id, character_id, created_at)
SELECT
    u.id,
    c.id,
    now() - ((8 - u.rn) || ' days')::interval
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    WHERE email LIKE 'user%@histar.vn'
    LIMIT 5
) u
CROSS JOIN LATERAL (
    SELECT id FROM characters
    WHERE location_id = '11111111-1111-1111-1111-111111111111'
    ORDER BY id LIMIT 1
) c
ON CONFLICT DO NOTHING;

INSERT INTO messages (conversation_id, role, content, created_at)
SELECT
    c.id,
    CASE WHEN g.i % 2 = 1 THEN 'user' ELSE 'assistant' END,
    CASE
        WHEN g.i % 2 = 1 THEN 'Chị ơi, bếp Hoàng Cầm là gì ạ?'
        ELSE 'Bếp Hoàng Cầm là loại bếp dưới lòng đất, nấu không khói — một trong ba trụ cột sinh tồn tại Củ Chi.'
    END,
    c.created_at + (g.i || ' minutes')::interval
FROM (
    SELECT id, created_at FROM conversations ORDER BY created_at LIMIT 5
) c
CROSS JOIN generate_series(1, 2) AS g(i);

-- Check-ins at Cu Chi
INSERT INTO checkins (user_id, location_id, latitude, longitude, created_at)
SELECT
    u.id,
    '11111111-1111-1111-1111-111111111111',
    11.143 + 0.0002,
    106.461 + 0.0002,
    now() - ((6 - u.rn) || ' days')::interval
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    LIMIT 5
) u
WHERE NOT EXISTS (
    SELECT 1 FROM checkins ck
    WHERE ck.user_id = u.id AND ck.location_id = '11111111-1111-1111-1111-111111111111'
);

-- User quest progress on main Cu Chi quest
INSERT INTO user_quest_progress (user_id, quest_id, status, started_at, completed_at)
SELECT
    u.id,
    '33333333-3333-3333-3333-333333333333',
    CASE WHEN u.rn <= 2 THEN 'completed' WHEN u.rn <= 4 THEN 'in_progress' ELSE 'not_started' END,
    now() - ((5 - u.rn) || ' days')::interval,
    CASE WHEN u.rn <= 2 THEN now() - ((4 - u.rn) || ' days')::interval ELSE NULL END
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    LIMIT 5
) u
ON CONFLICT (user_id, quest_id) DO NOTHING;

-- END schema: TimeLens_DB_Schema.sql

-- ============================================================================
-- BEGIN schema: 2026-06-02_fe_compat_migration.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\schema\2026-06-02_fe_compat_migration.sql
-- ============================================================================
-- FE compatibility migration pack (manual run, PostgreSQL)
-- Apply in non-prod first, then prod with backup.

BEGIN;

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS rating NUMERIC(2,1) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS is_ar_available BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS distance_km NUMERIC(5,2),
    ADD COLUMN IF NOT EXISTS sources TEXT,
    ADD COLUMN IF NOT EXISTS knowledge_context TEXT;

ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS steps_total INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS unlock_level INT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS cover_image TEXT,
    ADD COLUMN IF NOT EXISTS status_default VARCHAR(30) NOT NULL DEFAULT 'not_started',
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT now();

ALTER TABLE user_quest_progress
    ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    ADD COLUMN IF NOT EXISTS current_step INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS steps_total INT NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token VARCHAR(1024) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

COMMIT;

-- END schema: 2026-06-02_fe_compat_migration.sql

-- ============================================================================
-- BEGIN seed: 2026-06-02_fe_compat_indexes_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-02_fe_compat_indexes_seed.sql
-- ============================================================================
-- FE compatibility indexes + demo seeds (manual run, PostgreSQL)

BEGIN;

-- P1 index pack
CREATE INDEX IF NOT EXISTS idx_quests_location_created
    ON quests(location_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_uqp_user_status_created
    ON user_quest_progress(user_id, status, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_uqp_quest
    ON user_quest_progress(quest_id);

CREATE INDEX IF NOT EXISTS idx_locations_city_created
    ON locations(city, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_messages_conv_created
    ON messages(conversation_id, created_at ASC);

-- P2 demo seed alignment (idempotent upserts)
UPDATE locations
SET rating = 4.9, is_ar_available = TRUE
WHERE name = 'Dai Noi Hue';

UPDATE locations
SET rating = 4.8, is_ar_available = FALSE
WHERE name = 'Chua Thien Mu';

INSERT INTO quests (location_id, title, description, points_reward, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT l.id,
       'Dau an Hoang Thanh',
       'Giai ma cac co vat duoc tim thay tai khu vuc trung tam de khoi phuc dong thoi gian.',
       500,
       4,
       1,
       null,
       'not_started',
       now()
FROM locations l
WHERE l.name = 'Hoang Thanh Thang Long'
  AND NOT EXISTS (
    SELECT 1 FROM quests q WHERE q.title = 'Dau an Hoang Thanh'
);

INSERT INTO quests (location_id, title, description, points_reward, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT l.id,
       'Bi an Chua Cau',
       'Tim kiem cac dau vet thuong mai co dai doc theo bo song.',
       200,
       3,
       1,
       null,
       'not_started',
       now()
FROM locations l
WHERE l.name = 'Dai Noi Hue'
  AND NOT EXISTS (
    SELECT 1 FROM quests q WHERE q.title = 'Bi an Chua Cau'
);

COMMIT;

-- END seed: 2026-06-02_fe_compat_indexes_seed.sql

-- ============================================================================
-- BEGIN seed: 2026-06-02_fe_compat_data_topup.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-02_fe_compat_data_topup.sql
-- ============================================================================
-- FE data top-up — Cu Chi MVP only (no fake multi-site locations)
BEGIN;

-- Enrich canonical Cu Chi location for Explore/Home cards
UPDATE locations
SET
  description = COALESCE(
    NULLIF(description, ''),
    'Khu di tích lịch sử Địa đạo Củ Chi — hệ thống địa đạo 3 tầng, Bến Dược & Bến Đình.'
  ),
  city = 'TP.HCM',
  latitude = 11.143,
  longitude = 106.461,
  cover_image = '/media/cu-chi/map/hero.jpg',
  rating = COALESCE(rating, 4.8),
  is_ar_available = COALESCE(is_ar_available, TRUE),
  distance_km = NULL
WHERE id = '11111111-1111-1111-1111-111111111111';

-- Demo profiles for leaderboard (no fake locations)
WITH profile_count AS (
    SELECT COUNT(*) AS cnt FROM profiles
),
need AS (
    SELECT GREATEST(0, 8 - cnt) AS n FROM profile_count
)
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
SELECT
    'demo_user_' || gs.i || '@histar.vn',
    '$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe',
    'local',
    'USER',
    'Khám phá viên ' || gs.i,
    NULL,
    1 + (gs.i % 5),
    80 + gs.i * 45,
    'TP.HCM',
    now() - (gs.i || ' days')::interval
FROM need
JOIN LATERAL generate_series(1, need.n) gs(i) ON TRUE
ON CONFLICT (email) DO NOTHING;

-- Conversations: demo users x Chị Năm (Cu Chi)
WITH chi_nam AS (
    SELECT id FROM characters
    WHERE location_id = '11111111-1111-1111-1111-111111111111'
    ORDER BY id
    LIMIT 1
),
users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    WHERE email LIKE 'demo_user_%@histar.vn' OR email LIKE 'user%@histar.vn'
    LIMIT 5
)
INSERT INTO conversations (user_id, character_id, created_at)
SELECT u.id, c.id, now() - ((6 - u.rn) || ' days')::interval
FROM users u
CROSS JOIN chi_nam c
WHERE NOT EXISTS (
    SELECT 1 FROM conversations cv WHERE cv.user_id = u.id AND cv.character_id = c.id
);

-- Sample check-ins at Cu Chi only
WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    LIMIT 5
)
INSERT INTO checkins (user_id, location_id, latitude, longitude, created_at)
SELECT
    u.id,
    '11111111-1111-1111-1111-111111111111',
    11.1433,
    106.4613,
    now() - ((6 - u.rn) || ' days')::interval
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM checkins c
    WHERE c.user_id = u.id
      AND c.location_id = '11111111-1111-1111-1111-111111111111'
);

COMMIT;

-- END seed: 2026-06-02_fe_compat_data_topup.sql

-- ============================================================================
-- BEGIN seed: 2026-07-10_cu_chi_multi_panoramas.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-10_cu_chi_multi_panoramas.sql
-- ============================================================================
SET client_encoding TO 'UTF8';
-- Cu Chi multi-scene tour — generated from docs/cu-chi-tour-manifest.json
-- Route: 1 Bãi xe → 2 Đền → 3 Khu trưng bày → 4 Bộ Tư lệnh → 5 Khu ủy → 6 Nhà BD → 7 Tái hiện

-- Ensure tour360 columns exist before INSERT (Render seed runs this before Flyway V15)
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS area_slug VARCHAR(64);
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_yaw DOUBLE PRECISION DEFAULT 0;
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_pitch DOUBLE PRECISION DEFAULT 0;
ALTER TABLE hotspots ADD COLUMN IF NOT EXISTS marker_style VARCHAR(10);

-- Audit user discoveries (run manually on production before deploy)
-- SELECT discovery_key, COUNT(*) FROM user_discoveries
-- WHERE discovery_key LIKE 'scene:22222222-2222-2222-2222-%' GROUP BY discovery_key;

INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bai-xe/bai-xe-so-1.jpg', 'Bãi gửi xe gắn máy số 1', 'bai-xe', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t1-cong-vao.jpg', 'Cổng vào Đền Bến Dược', 'den-ben-duoc', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t2-bia.jpg', 'Trước bia tưởng niệm', 'den-ben-duoc', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t3.jpg', 'Trước đền (ghép panorama)', 'den-ben-duoc', 3, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222a', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t4.jpg', 'Trước đền', 'den-ben-duoc', 4, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222b', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t5.jpg', 'Bắc đền', 'den-ben-duoc', 5, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222c', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t6.jpg', 'Trong đền', 'den-ben-duoc', 6, 0, -0.05) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-trung-bay/kb-nga-ba.jpg', 'Ngã ba Khu trưng bày', 'khu-trung-bay', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222d', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-trung-bay/kb-trong-khu.jpg', 'Khu trưng bày', 'khu-trung-bay', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bo-tu-lenh/bo-tu-lenh.jpg', 'Căn cứ Bộ Tư lệnh QK SG-GĐ', 'bo-tu-lenh', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222e', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bo-tu-lenh/btl-trung-bay-2.jpg', 'Trưng bày bom & đạn', 'bo-tu-lenh', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-uy/khu-uy.jpg', 'Căn cứ Khu ủy QK SG-GĐ', 'khu-uy', 1, 0, -0.18) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/nha-bieu-dien/nha-bieu-dien.jpg', 'Nhà biểu diễn Sa bàn, Phim 3D', 'nha-bieu-dien', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-tai-hien/khu-tai-hien.jpg', 'Khu tái hiện Vùng Giải phóng', 'khu-tai-hien', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;

DELETE FROM hotspots WHERE type = 'scene' AND panorama_id IN ('22222222-2222-2222-2222-222222222221', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222228', '22222222-2222-2222-2222-222222222229', '22222222-2222-2222-2222-22222222222a', '22222222-2222-2222-2222-22222222222b', '22222222-2222-2222-2222-22222222222c', '22222222-2222-2222-2222-222222222224', '22222222-2222-2222-2222-22222222222d', '22222222-2222-2222-2222-222222222223', '22222222-2222-2222-2222-22222222222e', '22222222-2222-2222-2222-222222222225', '22222222-2222-2222-2222-222222222226', '22222222-2222-2222-2222-222222222227');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222221', 0.42, -0.55, 'scene', '22222222-2222-2222-2222-222222222222', '→ Đền Bến Dược', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222221' AND content_ref = '22222222-2222-2222-2222-222222222222' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222222', 2.69, -0.55, 'scene', '22222222-2222-2222-2222-222222222221', '→ Bãi xe', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222222' AND content_ref = '22222222-2222-2222-2222-222222222221' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222222', -2.69, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '→ Khu trưng bày', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222222' AND content_ref = '22222222-2222-2222-2222-222222222224' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222222', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222228', '→ Trong đền', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222222' AND content_ref = '22222222-2222-2222-2222-222222222228' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222228', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222229', '→ Trước đền', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222228' AND content_ref = '22222222-2222-2222-2222-222222222229' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222229', 3.14159, -0.42, 'scene', '22222222-2222-2222-2222-22222222222a', '→ Trước đền', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222229' AND content_ref = '22222222-2222-2222-2222-22222222222a' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222229', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222228', '← Quay lại', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222229' AND content_ref = '22222222-2222-2222-2222-222222222228' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222a', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-22222222222b', '→ Bắc đền', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222a' AND content_ref = '22222222-2222-2222-2222-22222222222b' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222a', 3.14159, -0.42, 'scene', '22222222-2222-2222-2222-222222222229', '← Quay lại', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222a' AND content_ref = '22222222-2222-2222-2222-222222222229' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222b', 0.12, -0.42, 'scene', '22222222-2222-2222-2222-22222222222c', '→ Trong đền', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222b' AND content_ref = '22222222-2222-2222-2222-22222222222c' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222224', 1.174, -0.55, 'scene', '22222222-2222-2222-2222-222222222222', '← Quay lại Đền', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222224' AND content_ref = '22222222-2222-2222-2222-222222222222' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222224', 1.674, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '→ Bộ Tư lệnh', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222224' AND content_ref = '22222222-2222-2222-2222-222222222223' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222224', -0.963, -0.55, 'scene', '22222222-2222-2222-2222-22222222222d', '→ Tham quan khu trưng bày', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222224' AND content_ref = '22222222-2222-2222-2222-22222222222d' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222d', 1.389, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '← Quay lại ngã ba', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222d' AND content_ref = '22222222-2222-2222-2222-222222222224' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222223', 0.55, -0.55, 'scene', '22222222-2222-2222-2222-222222222225', '→ Khu ủy', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222223' AND content_ref = '22222222-2222-2222-2222-222222222225' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222223', 0.85, -0.55, 'scene', '22222222-2222-2222-2222-22222222222e', '→ Trưng bày bom & đạn', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222223' AND content_ref = '22222222-2222-2222-2222-22222222222e' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222e', -0.85, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '← Quay lại', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222e' AND content_ref = '22222222-2222-2222-2222-222222222223' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222225', -0.35, -0.55, 'scene', '22222222-2222-2222-2222-222222222226', '→ Nhà biểu diễn', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222225' AND content_ref = '22222222-2222-2222-2222-222222222226' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222226', 0.22, -0.55, 'scene', '22222222-2222-2222-2222-222222222227', '→ Khu tái hiện', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222226' AND content_ref = '22222222-2222-2222-2222-222222222227' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222228', 3.141592653589793, -0.42, 'scene', '22222222-2222-2222-2222-222222222222', '← Quay lại', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222228' AND content_ref = '22222222-2222-2222-2222-222222222222' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222b', 3.141592653589793, -0.42, 'scene', '22222222-2222-2222-2222-22222222222a', '← Quay lại', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222b' AND content_ref = '22222222-2222-2222-2222-22222222222a' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-22222222222c', 3.2615926535897932, -0.42, 'scene', '22222222-2222-2222-2222-22222222222b', '← Quay lại', 'near' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-22222222222c' AND content_ref = '22222222-2222-2222-2222-22222222222b' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222223', 4.815592653589793, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '← Quay lại', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222223' AND content_ref = '22222222-2222-2222-2222-222222222224' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222225', 3.691592653589793, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '← Quay lại', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222225' AND content_ref = '22222222-2222-2222-2222-222222222223' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222226', 2.791592653589793, -0.55, 'scene', '22222222-2222-2222-2222-222222222225', '← Quay lại', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222226' AND content_ref = '22222222-2222-2222-2222-222222222225' AND type = 'scene');
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) SELECT '22222222-2222-2222-2222-222222222227', 3.3615926535897933, -0.55, 'scene', '22222222-2222-2222-2222-222222222226', '← Quay lại', 'far' WHERE NOT EXISTS (SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222227' AND content_ref = '22222222-2222-2222-2222-222222222226' AND type = 'scene');

-- END seed: 2026-07-10_cu_chi_multi_panoramas.sql

-- ============================================================================
-- BEGIN seed: 2026-06-11_cp3_gamification_upgrade.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-11_cp3_gamification_upgrade.sql
-- ============================================================================
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

-- END seed: 2026-06-11_cp3_gamification_upgrade.sql

-- ============================================================================
-- BEGIN seed: 2026-06-11_admin_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-11_admin_seed.sql
-- ============================================================================
-- Platform admin account (dev/demo only — change password on production)
-- Same password hash as seed users in TimeLens_DB_Schema.sql (register new password in prod)

INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
VALUES (
    'admin@histar.vn',
    '$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe',
    'local',
    'ADMIN',
    'Platform Admin',
    NULL,
    1,
    0,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role;

-- END seed: 2026-06-11_admin_seed.sql

-- ============================================================================
-- BEGIN seed: 2026-06-11_unlock_rules.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-11_unlock_rules.sql
-- ============================================================================
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

-- END seed: 2026-06-11_unlock_rules.sql

-- ============================================================================
-- BEGIN seed: 2026-06-12_photo_scene_unlock_keys.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-12_photo_scene_unlock_keys.sql
-- ============================================================================
-- Add unlock_key to photo_scenes for Time Portal discovery alignment
BEGIN;

ALTER TABLE photo_scenes ADD COLUMN IF NOT EXISTS unlock_key VARCHAR(128);

UPDATE photo_scenes SET unlock_key = 'photo:cua-ham'
WHERE id = '50000001-0000-4000-8000-000000000001';

UPDATE photo_scenes SET unlock_key = 'scene:22222222-2222-2222-2222-222222222221'
WHERE id = '50000001-0000-4000-8000-000000000002';

UPDATE photo_scenes SET unlock_key = 'scene:22222222-2222-2222-2222-222222222223'
WHERE id = '50000001-0000-4000-8000-000000000003';

UPDATE photo_scenes SET unlock_key = 'hotspot:vent'
WHERE id = '50000001-0000-4000-8000-000000000004';

UPDATE photo_scenes SET unlock_key = 'photo:gieng'
WHERE id = '50000001-0000-4000-8000-000000000005';

COMMIT;

-- END seed: 2026-06-12_photo_scene_unlock_keys.sql

-- ============================================================================
-- BEGIN seed: 2026-06-12_discovery_artifact_links.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-12_discovery_artifact_links.sql
-- ============================================================================
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

-- END seed: 2026-06-12_discovery_artifact_links.sql

-- ============================================================================
-- BEGIN seed: 2026-06-12_quest_completion_trigger.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-12_quest_completion_trigger.sql
-- ============================================================================
-- Quest completion trigger for check-in evaluator
BEGIN;

ALTER TABLE quests ADD COLUMN IF NOT EXISTS completion_trigger VARCHAR(32) NOT NULL DEFAULT 'checkin';

UPDATE quests SET completion_trigger = 'checkin' WHERE completion_trigger IS NULL;

COMMIT;

-- END seed: 2026-06-12_quest_completion_trigger.sql

-- ============================================================================
-- BEGIN seed: 2026-06-12_value_layer_upgrade.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-12_value_layer_upgrade.sql
-- ============================================================================
-- HistAR Value Layer upgrade (visit_sessions, quest steps, bindings, composite artifacts, analytics events, B2B2E stub)

-- Quest multi-step: comma-separated discovery keys before check-in complete
ALTER TABLE quests ADD COLUMN IF NOT EXISTS step_discovery_keys TEXT;

UPDATE quests
SET step_discovery_keys = 'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223,era:1948'
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND step_discovery_keys IS NULL;

-- Visit sessions (journey / B2B analytics foundation)
CREATE TABLE IF NOT EXISTS visit_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES profiles(id),
    location_id     UUID NOT NULL REFERENCES locations(id),
    mode            VARCHAR(16) NOT NULL DEFAULT 'online',
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at        TIMESTAMPTZ,
    checkin_id      UUID REFERENCES checkins(id)
);
CREATE INDEX IF NOT EXISTS idx_visit_sessions_location_started ON visit_sessions(location_id, started_at);
CREATE INDEX IF NOT EXISTS idx_visit_sessions_user ON visit_sessions(user_id, started_at DESC);

CREATE TABLE IF NOT EXISTS visit_session_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_session_id   UUID NOT NULL REFERENCES visit_sessions(id) ON DELETE CASCADE,
    event_type         VARCHAR(32) NOT NULL,
    event_key          VARCHAR(128),
    source             VARCHAR(32),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_visit_session_events_session ON visit_session_events(visit_session_id, created_at);

-- Multi-site discovery bindings (replaces FE hardcode over time)
CREATE TABLE IF NOT EXISTS discovery_content_bindings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id     UUID NOT NULL REFERENCES locations(id),
    unlock_key        VARCHAR(128) NOT NULL,
    record_key        VARCHAR(128) NOT NULL,
    engagement      VARCHAR(32) NOT NULL,
    href_template     TEXT NOT NULL,
    sort_order        INT DEFAULT 0,
    UNIQUE (location_id, unlock_key)
);

-- Composite artifact unlock requirements
CREATE TABLE IF NOT EXISTS artifact_unlock_requirements (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id             UUID NOT NULL REFERENCES locations(id),
    artifact_unlock_key       VARCHAR(128) NOT NULL,
    required_discovery_key  VARCHAR(128) NOT NULL,
    UNIQUE (location_id, artifact_unlock_key, required_discovery_key)
);

-- 3-layer analytics events (impression / engagement / discovery)
CREATE TABLE IF NOT EXISTS analytics_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES profiles(id),
    location_id     UUID REFERENCES locations(id),
    visit_session_id UUID REFERENCES visit_sessions(id),
    event_type      VARCHAR(32) NOT NULL,
    event_key       VARCHAR(128),
    source          VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_analytics_events_location ON analytics_events(location_id, event_type, created_at);

-- B2B2E stub (organizations)
CREATE TABLE IF NOT EXISTS organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug        VARCHAR(64) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    plan        VARCHAR(32) NOT NULL DEFAULT 'trial',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS organization_members (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations(id),
    user_id          UUID NOT NULL REFERENCES profiles(id),
    org_role         VARCHAR(16) NOT NULL,
    invited_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at      TIMESTAMPTZ,
    UNIQUE (organization_id, user_id)
);

INSERT INTO organizations (id, slug, name, plan)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'cu-chi-demo', 'Địa đạo Củ Chi (Demo)', 'trial')
ON CONFLICT (slug) DO NOTHING;

-- END seed: 2026-06-12_value_layer_upgrade.sql

-- ============================================================================
-- BEGIN seed: 2026-06-13_phase_b_upgrade.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-13_phase_b_upgrade.sql
-- ============================================================================
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

-- END seed: 2026-06-13_phase_b_upgrade.sql

-- ============================================================================
-- BEGIN seed: 2026-06-14_phase1_hardening.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-14_phase1_hardening.sql
-- ============================================================================
-- Phase 1 hardening: quest unique guard, session activity, ended_reason

CREATE UNIQUE INDEX IF NOT EXISTS uq_user_quest_progress_user_quest
  ON user_quest_progress (user_id, quest_id);

ALTER TABLE visit_sessions ADD COLUMN IF NOT EXISTS last_activity_at TIMESTAMPTZ;
UPDATE visit_sessions SET last_activity_at = COALESCE(ended_at, started_at) WHERE last_activity_at IS NULL;

ALTER TABLE visit_sessions ADD COLUMN IF NOT EXISTS ended_reason VARCHAR(20);

-- END seed: 2026-06-14_phase1_hardening.sql

-- ============================================================================
-- BEGIN seed: 2026-06-15_visit_session_event_snapshot.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-15_visit_session_event_snapshot.sql
-- ============================================================================
-- POI name snapshot for session replay (immutable label at event time)
ALTER TABLE visit_session_events
  ADD COLUMN IF NOT EXISTS poi_name_snapshot VARCHAR(255);

-- END seed: 2026-06-15_visit_session_event_snapshot.sql

-- ============================================================================
-- BEGIN seed: 2026-06-16_analytics_event_metadata.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-16_analytics_event_metadata.sql
-- ============================================================================
ALTER TABLE analytics_events ADD COLUMN IF NOT EXISTS metadata TEXT;

-- END seed: 2026-06-16_analytics_event_metadata.sql

-- ============================================================================
-- BEGIN seed: 2026-06-17_cu_chi_artifacts_story_admin.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-17_cu_chi_artifacts_story_admin.sql
-- ============================================================================
-- Cu Chi: artifact story column, 3 missing artifacts, discovery links, demo admin account
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS story TEXT;

-- STT 19: Xưởng chế tạo vũ khí ngầm
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000018',
  '11111111-1111-1111-1111-111111111111',
  'Xưởng chế tạo vũ khí ngầm',
  '/media/cu-chi/artifacts/trung-bay-vu-khi.png',
  'Cưa bom lép lấy TNT — lấy vũ khí địch đánh địch.',
  'Giữa lòng đất Củ Chi thiếu thốn trăm bề, các chiến sĩ công binh đã thực hiện những nhiệm vụ nghẹt thở: Cưa bom lép của Mỹ để lấy thuốc nổ (TNT). Mỹ trút xuống Củ Chi hàng triệu tấn bom đạn, trong đó có một tỷ lệ nhỏ bị tịt (bom lép). Các chiến sĩ phải dùng cưa tay, vừa cưa vừa dội nước cực kỳ chậm rãi để tránh phát ra tia lửa gây nổ.

Với phương châm ''lấy vũ khí địch đánh địch'', từ nguồn thuốc nổ này, xưởng vũ khí ngầm ở tầng 2 đã đúc thành công lựu đạn chày, mìn định hướng (gạt chân), và đặc biệt là Mìn gạt chống tăng của Dũng sĩ Tô Hoài Đức. Những vũ khí tự chế thô sơ này chính là khắc tinh bẻ gãy các chiến dịch càn quét bằng cơ giới quy mô lớn như Crimp (1966) hay Cedar Falls (1967) của quân đội Mỹ.',
  'artifact:vu-khi',
  'primary',
  18
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- STT 20: Trạm xá hầm phẫu thuật
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000019',
  '11111111-1111-1111-1111-111111111111',
  'Trạm xá hầm phẫu thuật',
  '/media/cu-chi/artifacts/den-dau.jpg',
  'Cấp cứu dưới lòng đất — đèn dầu, thiếu thuốc, không khí ẩm.',
  'Trạm xá dưới lòng đất là nơi ranh giới giữa sự sống và cái chết mong manh nhất. Nằm sâu ở tầng 2, trong không gian ẩm ướt và thiếu dưỡng khí, các bác sĩ phải thực hiện những ca phẫu thuật phức tạp dưới ánh sáng leo lét của đèn dầu hoặc đèn pin quay tay bằng cơ học.

Điều kiện y tế vô cùng khắc nghiệt: Thiếu thốn thuốc gây mê, thuốc kháng sinh và băng gạc vô trùng. Để giữ vệ sinh, căn hầm phẫu thuật được lót xung quanh bằng các tấm nilon hoặc vải dù chiếm được của địch. Nhiều ca cưa chân, gắp mảnh bom, các y bác sĩ phải động viên chiến sĩ bằng tinh thần và lòng quả cảm. Nơi đây không chỉ cứu chữa hàng ngàn thương bệnh binh mà còn là nơi chứng kiến nhiều em bé Củ Chi cất tiếng khóc chào đời ngay trong lòng đất thép.',
  'artifact:tram-xa',
  'primary',
  19
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- STT 21: Khu trưng bày bẫy rập
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000020',
  '11111111-1111-1111-1111-111111111111',
  'Khu trưng bày bẫy rập',
  '/media/cu-chi/artifacts/chong-tre.jpg',
  'Chông nắp, hom, cần — đòn đánh tâm lý nặng nề đối với quân viễn chinh.',
  'Trận đồ bẫy du kích tại Củ Chi là nỗi khiếp sợ hoảng loạn và là đòn đánh tâm lý nặng nề nhất đối với quân viễn chinh Mỹ. Lợi dụng địa hình rừng bụi rậm rạp, quân dân ta đã chế tạo ra hàng chục loại bẫy từ các nguyên liệu thô sơ như tre già ngâm nước (cho cứng và độc), sắt phế liệu, đinh đỉa.

Các loại bẫy tiêu biểu gồm: Chông nắp (địch giẫm lên nắp sẽ lật úp, rơi xuống hố chông), Chông hom (đâm ngược khi rút chân lên), Chông cần (khi vướng dây, cần tre kéo cọc chông đâm ngang hông). Bẫy không chỉ bố trí trên mặt đất mà còn cài cắm ngay tại các góc cua hẹp của địa đạo dưới hầm (hầm chữ Z) để chống lại lính ''chuột cống''. Địch một khi lọt vào trận đồ bẫy sẽ buộc phải dừng toàn bộ cuộc càn quét để cáng tải thương binh, làm tiêu hao sinh lực và kéo sụp ý chí tiến công.',
  'artifact:bay',
  'primary',
  20
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- Discovery → artifact bridges for new POIs
INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('artifact:vu-khi', 'artifact:vu-khi', '11111111-1111-1111-1111-111111111111'),
('artifact:vu-khi', 'artifact:min-gat', '11111111-1111-1111-1111-111111111111'),
('artifact:vu-khi', 'artifact:cua-bom', '11111111-1111-1111-1111-111111111111'),
('artifact:tram-xa', 'artifact:tram-xa', '11111111-1111-1111-1111-111111111111'),
('artifact:tram-xa', 'artifact:den-dau', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:bay', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:chong-tre', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:lao-tre', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

-- Story for Bếp Hoàng Cầm (existing)
UPDATE artifacts SET story = 'Chiến sĩ nuôi quân Hoàng Cầm sáng chế loại bếp dưới lòng đất từ năm 1951. Tại Địa đạo Củ Chi, bếp được đào vào sườn đất tầng hai — nơi chiến sĩ nấu ăn nuôi hàng nghìn người sống trong hầm.

Từ lò bếp, các đường rãnh thoát khói dài tỏa ra xa như "râu mực", phủ đất ẩm để lọc khói. Phần khói lên mặt đất chỉ còn làn hơi loãng như sương sớm — máy bay trinh sát Mỹ khó phát hiện. Bếp Hoàng Cầm góp phần thực hiện phương châm "nấu không khói" — một trong ba trụ cột sinh tồn: Đi không dấu, nấu không khói, nói không tiếng.'
WHERE unlock_key = 'hotspot:kitchen' AND location_id = '11111111-1111-1111-1111-111111111111';

-- Demo admin: xem toàn bộ cổ vật & discovery (role ADMIN + full seed unlock)
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
VALUES (
    'demo@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'ADMIN',
    'Demo Admin — Xem tất cả',
    NULL,
    10,
    999,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET
    role = 'ADMIN',
    display_name = EXCLUDED.display_name,
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    level = GREATEST(profiles.level, 10),
    total_points = GREATEST(profiles.total_points, 999);

-- Legacy platform admin (same password)
UPDATE profiles
SET password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'ADMIN',
    display_name = 'Platform Admin'
WHERE email = 'admin@histar.vn';

INSERT INTO user_artifacts (user_id, artifact_id, unlocked_at)
SELECT p.id, a.id, now()
FROM profiles p
CROSS JOIN artifacts a
WHERE p.email IN ('demo@histar.vn', 'admin@histar.vn')
  AND a.location_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id, artifact_id) DO NOTHING;

INSERT INTO user_discoveries (user_id, location_id, discovery_key, discovered_at)
SELECT p.id, dp.location_id, dp.unlock_key, now()
FROM profiles p
CROSS JOIN discovery_points dp
WHERE p.email IN ('demo@histar.vn', 'admin@histar.vn')
  AND dp.location_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id, location_id, discovery_key) DO NOTHING;

COMMIT;

-- END seed: 2026-06-17_cu_chi_artifacts_story_admin.sql

-- ============================================================================
-- BEGIN seed: 2026-06-18_cu_chi_only_cleanup.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-18_cu_chi_only_cleanup.sql
-- ============================================================================
-- Cu Chi only: remove fake / placeholder locations and related demo data
-- Keeps location 11111111-1111-1111-1111-111111111111 (Địa đạo Củ Chi)
BEGIN;

-- Normalize canonical Cu Chi row
UPDATE locations
SET
  name = 'Địa đạo Củ Chi',
  description = 'Khu di tích lịch sử Địa đạo Củ Chi — hệ thống hầm ngầm thời kháng chiến tại huyện Củ Chi, TP.HCM.',
  city = 'TP.HCM',
  latitude = 11.143,
  longitude = 106.461,
  cover_image = COALESCE(NULLIF(cover_image, ''), '/media/cu-chi/map/hero.jpg')
WHERE id = '11111111-1111-1111-1111-111111111111';

-- Clear FK blockers (visit_sessions has no ON DELETE CASCADE)
DELETE FROM analytics_events
WHERE location_id IS NOT NULL
  AND location_id <> '11111111-1111-1111-1111-111111111111';

DELETE FROM visit_session_events
WHERE visit_session_id IN (
  SELECT id FROM visit_sessions
  WHERE location_id <> '11111111-1111-1111-1111-111111111111'
);

DELETE FROM visit_sessions
WHERE location_id <> '11111111-1111-1111-1111-111111111111';

DELETE FROM discovery_content_bindings
WHERE location_id <> '11111111-1111-1111-1111-111111111111';

DELETE FROM artifact_unlock_requirements
WHERE location_id <> '11111111-1111-1111-1111-111111111111';

DELETE FROM user_quest_progress
WHERE location_id IS NOT NULL
  AND location_id <> '11111111-1111-1111-1111-111111111111';

-- Remove duplicate Cu Chi rows (from fe_compat topup)
DELETE FROM locations
WHERE lower(name) LIKE '%cu chi%'
  AND id <> '11111111-1111-1111-1111-111111111111';

-- Remove all non–Cu Chi locations (cascades to quests, characters, panoramas, etc.)
DELETE FROM locations
WHERE id <> '11111111-1111-1111-1111-111111111111';

-- Fake location names that might remain
DELETE FROM locations
WHERE name ILIKE 'Di tích lịch sử #%'
   OR name ILIKE 'Demo Di Tich #%'
   OR name ILIKE 'Dai Noi%'
   OR name ILIKE 'Chua Thien%'
   OR name ILIKE 'Hoang Thanh%'
   OR name ILIKE 'Van Mieu%'
   OR name ILIKE 'Pho Co%'
   OR name ILIKE 'Thanh Nha%'
   OR name ILIKE 'Co Do%'
   OR name ILIKE 'Den Hung%'
   OR name ILIKE 'Ben Nha Rong%';

-- Characters: keep only Cu Chi personas
DELETE FROM characters
WHERE location_id <> '11111111-1111-1111-1111-111111111111'
   OR name ILIKE 'Nhân vật lịch sử #%'
   OR name ILIKE 'Nhan vat lich su #%';

-- Quests: keep Cu Chi main quest only
DELETE FROM quests
WHERE location_id <> '11111111-1111-1111-1111-111111111111'
   OR title ILIKE 'Nhiệm vụ #%'
   OR title ILIKE 'Nhiem vu demo #%'
   OR title IN ('Dau an Hoang Thanh', 'Bi an Chua Cau', 'Mat ma Lang Tam');

-- Badges: keep 3 Cu Chi seed badges
DELETE FROM badges
WHERE name ILIKE 'Huy hiệu #%'
   OR name ILIKE 'Huy hieu demo #%';

-- Photo frames: keep 3 Cu Chi frames
DELETE FROM photo_frames
WHERE name ILIKE 'Khung lịch sử #%';

-- Campaigns demo
DELETE FROM campaigns WHERE name ILIKE 'Chiến dịch #%';

-- Progress tied to deleted quests (if FK did not cascade)
DELETE FROM user_quest_progress uqp
WHERE NOT EXISTS (SELECT 1 FROM quests q WHERE q.id = uqp.quest_id);

COMMIT;

-- END seed: 2026-06-18_cu_chi_only_cleanup.sql

-- ============================================================================
-- BEGIN seed: 2026-06-19_heritage_sites_from_dataset.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-19_heritage_sites_from_dataset.sql
-- ============================================================================
-- Heritage sites: FULL content from Dataset docx (locations + parsed artifacts + stories)
BEGIN;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222201', 'Bến Nhà Rồng', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 01 tòa nhà Nguyễn Tất Thành, Phường 12, Quận 4, TP. Hồ Chí Minh, ngay ngã ba sông Sài Gòn và kênh Bến Nghé.
Tên gọi chính thức hiện nay: Bảo tàng Hồ Chí Minh – Chi nhánh TP. Hồ Chí Minh.
Tầm vóc: Đây là một cụm di tích kiến trúc - lịch sử đặc biệt. Nơi đây không chỉ lưu giữ cấu trúc thương cảng kiểu Pháp bề thế mà còn là "địa chỉ đỏ" gìn giữ hơn 20.000 tài liệu, hiện vật về cuộc đời và sự nghiệp cách mạng của Bác Hồ.
2. Lịch sử hình thành: Từ trụ sở thương cảng đến chứng nhân lịch sử
Xây dựng (1862 - 1863): Sau khi chiếm được Gia Định, thực dân Pháp cho xây dựng tòa nhà này để làm trụ sở cho Hãng vận tải đường biển Hoàng gia Pháp (Messageries Maritimes). Đây là một trong những công trình kiến trúc phương Tây đầu tiên được xây dựng tại Sài Gòn.
Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:
"Tái hiện con tàu lịch sử" bằng AR:
Khi đứng tại khu vực bến cảng hướng ra sông Sài Gòn, du khách có thể sử dụng điện thoại thông minh quét mã AR để "triệu hồi" hình ảnh 3D của con tàu buôn cổ Amiral Latouche-Tréville đang neo đậu ngay trên bến sông thật.
Ứng dụng AR sẽ mô phỏng lại khoảnh khắc người thanh niên Văn Ba bước lên mạn tàu, chào tạm biệt bến cảng Sài Gòn trong không gian chuyển động 3D, giúp người xem cảm nhận rõ nét bối cảnh lịch sử của ngày 5/6 năm ấy.
Số hóa bản đồ "Hành trình 30 năm vạn dặm": Bảo tàng trang bị hệ thống màn hình cảm ứng tương tác cỡ lớn, số hóa toàn bộ bản đồ thế giới ghi dấu những nơi Bác đã đi qua. Du khách chỉ cần chạm vào các quốc gia như Pháp, Anh, Nga... bản đồ AR sẽ hiển thị các tư liệu ảnh, bài báo, hiện vật liên quan đến hoạt động của Bác tại quốc gia đó kèm giọng thuyết minh tự động.
Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 10.768, 106.707, 'TP.HCM', '/media/heritage/ben-nha-rong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Bến Nhà Rồng
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000001-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Hai con rồng trên mái Nhà Rồng', '/media/heritage/ben-nha-rong/cover.webp', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc…', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-rong-mai', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000002-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Con tàu Amiral Latouche-Tréville', '/media/heritage/ben-nha-rong/cover.webp', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm…', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-tau-amiral', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000003-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Chiếc vali mây của Bác', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-vali-may', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000004-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Đôi dép cao su', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-dep-cao-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222202', 'Chùa thiên mụ', '1. Tổng quan & Vị trí
Vị trí: Chùa nằm trên đồi Hà Khê, thuộc tả ngạn Sông Hương, cách trung tâm thành phố Huế khoảng 5km về phía Tây.
Kiến trúc cảnh quan: Ngôi chùa soi bóng xuống dòng sông Hương thơ mộng, tạo nên một bức tranh sơn thủy hữu tình, mang đậm nét u tịch, trang nghiêm và cổ kính.
2. Lịch sử hình thành và những truyền thuyết
Năm thành lập: Chùa được chính thức khởi công xây dựng vào năm 1601 (tức năm Tân Sửu) bởi vị chúa Nguyễn đầu tiên – Chúa Tiên Nguyễn Hoàng.
Truyền thuyết về tên gọi "Thiên Mụ":
Theo đại nam nhất thống chí, khi chúa Nguyễn Hoàng vào trấn thủ xứ Thuận Hóa, ông đã đích thân đi dọc bờ sông Hương để xem xét địa hình nhằm chuẩn bị cho việc mở mang cơ nghiệp. Khi đến đồi Hà Khê, ông nghe người dân địa phương kể lại rằng: Đêm đêm thường có một bà lão mặc áo đỏ quần lục xuất hiện trên đồi, nói với mọi người rằng "Rồi đây sẽ có một vị chúa đến lập chùa để tụ khí cho bền long mạch, làm cho nước Nam hùng mạnh".
Nói xong, bà lão biến mất vào đám mây. Cho rằng đó là điềm lành từ trời, Nguyễn Hoàng đã cho dựng chùa trên đồi và đặt tên là Thiên Mụ Tự (Chùa Bà Mụ Trời).
Các mốc thời gian quan trọng:
Năm 1665: Chúa Nguyễn Phúc Tần cho trùng tu, mở rộng quy mô chùa.
Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:
Tháp Phước Duyên:
Xây dựng năm 1844 dưới thời vua Thiệu Trị, ban đầu có tên là tháp Từ Nhân.
Tháp cao 7 tầng (khoảng 21m), hình bát giác, mỗi tầng thờ một Đức Phật khác nhau. Đây là một trong những ngọn tháp cổ mang tính biểu tượng cao nhất tại Việt Nam.
Đại Hồng Chung (Chuông đồng):
Được chúa Nguyễn Phúc Chu cho đúc vào năm 1710.
Chuông cao 2,5m, đường kính miệng chuông 1,4m và nặng tới 3.285 cân (khoảng hơn 2 tấn). Tiếng chuông Thiên Mụ từ lâu đã đi vào ca dao, văn học vì độ vang xa và trầm ấm âm vang khắp vùng kinh thành xưa.
Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.
Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.
Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 16.453, 107.558, 'Huế', '/media/heritage/chua-thien-mu/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Chùa thiên mụ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000005-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Tháp Phước Duyên', '/media/heritage/chua-thien-mu/cover.webp', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó…', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-thap-phuoc-duyen', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000006-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Đại Hồng Chung', '/media/heritage/chua-thien-mu/cover.webp', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các…', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-dai-hong-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000007-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Bia đá thời Chúa Nguyễn Phúc Chu', '/media/heritage/chua-thien-mu/cover.webp', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'artifact:chua-thien-mu-bia-da', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000008-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Điện Đại Hùng', '/media/heritage/chua-thien-mu/cover.webp', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'artifact:chua-thien-mu-dien-dai-hung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000009-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Xe Austin Thích Quảng Đức', '/media/heritage/chua-thien-mu/cover.webp', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản…', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 'artifact:chua-thien-mu-xe-austin', 'primary', 5)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222203', 'Cố đô Hoa Lư', '1. Tổng quan & Vị trí
Vị trí: Thuộc xã Trường Yên, huyện Hoa Lư, tỉnh Ninh Bình, cách thủ đô Hà Nội khoảng 90km về phía Nam.
Tầm vóc: Hoa Lư là một trong ba vùng lõi cấu thành nên Quần thể danh thắng Tràng An – Di sản Văn hóa và Thiên nhiên thế giới (di sản hỗn hợp duy nhất tại Đông Nam Á) được UNESCO công nhận vào năm 2014.
Địa thế phong thủy: Kinh đô được bao bọc bởi những dãy núi đá vôi trùng điệp, tạo thành một bức tường thành thiên nhiên kiên cố, xen kẽ giữa các thung lũng là hệ thống sông ngòi chằng chịt, mang tính chất một thành trì quân sự hiểm yếu "bất khả xâm phạm".
2. Lịch sử hình thành: Kinh đô của ba triều đại
Cố đô Hoa Lư đóng vai trò là thủ đô của nước Đại Cồ Việt trong suốt 42 năm (968 – 1010), gắn liền với sự nghiệp của ba triều đại liên tiếp:
Nhà Đinh (968 - 980): Sau khi dùng mưu lược và võ công dẹp loạn 12 sứ quân, thống nhất giang sơn, Đinh Bộ Lĩnh lên ngôi hoàng đế (Đinh Tiên Hoàng), đặt tên nước là Đại Cồ Việt và chọn Hoa Lư làm đất định đô. Ông cho đắp thành, đào hào, xây dựng cung điện.
Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:
Đền vua Đinh Tiên Hoàng:
Nằm ở trung tâm thành Đông, xây dựng theo kiểu "Nội công ngoại quốc" đặc trưng.
Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.
Đền vua Lê Đại Hành:
Cách đền vua Đinh khoảng 300m, quy mô nhỏ hơn một chút nhưng nghệ thuật điêu khắc gỗ và đá vô cùng đặc sắc.
Nơi đây còn lưu giữ những mảng chạm khắc rồng, mây mang đậm phong cách nghệ thuật thời hậu Lê. Đặc biệt là bức tượng vua Lê Đại Hành, thái hậu Dương Vân Nga và kiệu rước cổ.
Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.
Khu khai quật khảo cổ học lòng đất:
Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 20.283, 105.917, 'Ninh Bình', '/media/heritage/co-do-hoa-lu/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Cố đô Hoa Lư
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000010-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Long Sàng đá', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'artifact:co-do-hoa-lu-long-sang', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000011-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Cột kinh Chùa Nhất Trụ', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'artifact:co-do-hoa-lu-cot-kinh', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000012-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Đền vua Lê Đại Hành', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái…', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:', 'artifact:co-do-hoa-lu-den-le-dai-hanh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000013-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Tiền cổ Đinh Vạn Tuế', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung…', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 'artifact:co-do-hoa-lu-tien-co', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222204', 'Hoàng Thành Thăng Long', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 19C Hoàng Diệu, quận Ba Đình, Hà Nội. Khu di tích có diện tích cực kỳ rộng lớn, bao quanh bởi các con đường lịch sử như Hoàng Diệu, Điện Biên Phủ, Phan Đình Phùng và Nguyễn Tri Phương.
Tầm vóc: Đây là quần thể di tích gắn liền với lịch sử kinh thành Thăng Long - Đông Kinh và tỉnh thành Hà Nội. Đây là minh chứng sống động cho sự phát triển liên tục của trung tâm quyền lực chính trị Việt Nam suốt 13 thế kỷ (từ thời tiền Thăng Long thế kỷ VII, qua các triều đại Lý, Trần, Lê, Mạc, Nguyễn đến thời đại Hồ Chí Minh). Khu trung tâm Hoàng thành Thăng Long được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 2010.
2. Lịch sử hình thành qua các tầng đất khảo cổ
Điểm đặc biệt nhất của Hoàng Thành Thăng Long chính là "lịch sử chồng chồng lên nhau" ngay trong lòng đất:
Thời kỳ tiền Thăng Long (Thế kỷ VII - IX): Nơi đây từng là trụ sở của An Nam đô hộ phủ thời nhà Đường (thành Đại La).
Định đô Thăng Long (Năm 1010): Vua Lý Thái Tổ dời đô từ Hoa Lư về Đại La, đổi tên thành Thăng Long và cho xây dựng Hoàng Thành trên nền thành cũ.
Các triều đại tiếp nối (Trần, Lê, Mạc): Tiếp tục mở rộng, xây dựng thêm nhiều cung điện nguy nga. Nơi đây là trung tâm đầu não điều hành đất nước, kinh qua các cuộc chiến chống ngoại xâm hiển hách.
Thời nhà Nguyễn (Thế kỷ XIX): Khi triều Nguyễn chuyển kinh đô vào Phú Xuân (Huế), nơi đây trở thành thành Hà Nội. Vua Gia Long cho phá dỡ một số công trình cũ để xây lại thành theo kiến trúc Vauban của Pháp.
Thời kỳ hiện đại: Trong kháng chiến chống Mỹ, Tổng hành dinh Quân đội Nhân dân Việt Nam (Bộ Chính trị và Quân ủy Trung ương) đã chọn nơi đây làm trung tâm chỉ huy đầu não nhờ hệ thống hầm ngầm kiên cố.
3. Các công trình tiêu biểu & Cổ vật nghìn năm
Khu di tích được chia làm hai khu vực chính: Khu di tích lịch sử Thành cổ Hà Nội và Khu khảo cổ học 18 Hoàng Diệu.
A. Các công trình lộ thiên nổi bật
Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.
Cổ vật hoàng cung:
Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 21.036, 105.817, 'Hà Nội', '/media/heritage/hoang-thanh-thang-long/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Hoàng Thành Thăng Long
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000014-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Cột cờ Hà Nội (Kỳ Đài)', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời…', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-cot-co', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000015-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Đôi rồng đá Điện Kính Thiên', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc…', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-doi-rong-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000016-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Ngói chim phượng thời Lý', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn…', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.', 'artifact:hoang-thanh-thang-long-ngoi-phuong', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000017-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Bát sứ ngự dụng', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế,…', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 'artifact:hoang-thanh-thang-long-bat-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222205', 'Phố Cổ Hội An', '1. Tổng quan & Vị trí
Vị trí: Nằm ở hạ lưu sông Thu Bồn, thuộc địa phận thành phố Hội An, tỉnh Quảng Nam, cách thành phố Đà Nẵng khoảng 30km về phía Nam.
Tầm vóc: Hội An là một ví dụ đặc biệt xuất sắc về một thương cảng truyền thống ở Đông Nam Á được bảo tồn nguyên vẹn gần như cấu trúc đô thị ban đầu. Với những giá trị văn hóa, kiến trúc vật thể và phi vật thể vô giá, Phố cổ Hội An đã được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 1999.
2. Lịch sử hình thành: Từ cảng thị Champa đến thương cảng quốc tế
Thời kỳ Tiền Hội An: Từ thế kỷ thứ II đến thế kỷ XV, nơi đây từng là cảng biển sầm uất của vương quốc Champa (thời kỳ Lâm Ấp phố), đóng vai trò quan trọng trên Con đường tơ lụa và Con đường gia vị trên biển.
Thời kỳ Hưng thịnh (Thế kỷ XVI - XVIII): Dưới thời các Chúa Nguyễn mở mang bờ cõi về phía Nam (Đàng Trong), Hội An phát triển thành thương cảng quốc tế lớn nhất khu vực. Các thuyền buôn từ Nhật Bản, Trung Quốc, Bồ Đào Nha, Hà Lan, Ấn Độ... tấp nập đến đây trao đổi hàng hóa (lụa là, gốm sứ, trầm hương, lâm sản).
Sự giao thoa văn hóa: Trong thời kỳ này, nhiều thương nhân Trung Quốc và Nhật Bản đã được phép định cư, xây dựng các khu phố, hội quán và nhà ở theo phong cách riêng của họ, hòa quyện với nền tảng kiến trúc truyền thống của người Việt, tạo nên một diện mạo đô thị độc nhất vô nhị.
Thời kỳ suy thoái & May mắn của lịch sử: Đến thế kỷ XIX, do sông Thu Bồn bị bồi lấp trầm tích và nhà Nguyễn thực hiện chính sách đóng cửa (bế quan tỏa cảng), cộng thêm sự trỗi dậy của cảng biển Đà Nẵng do người Pháp xây dựng, Hội An dần mất đi vị thế kinh tế. Tuy nhiên, chính sự "lãng quên" này lại giúp Hội An may mắn thoát khỏi quá trình đô thị hóa và sự tàn phá của chiến tranh để giữ gìn nguyên vẹn cấu trúc khu phố cổ cho đến ngày nay.
3. Kiến trúc giao thoa & Những di sản kiến trúc nổi bật
Kiến trúc Hội An mang đặc trưng với những ngôi nhà hình ống (bề ngang hẹp, chiều sâu rất dài) tường sơn màu vàng rơm nghệ thuật, mái lợp ngói âm dương và cấu trúc khung gỗ chịu lực vững chắc.
Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.
Các Hội quán của người Hoa:
Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.
Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.
Trải nghiệm AR (Thực tế tăng cường) "Xuyên không về quá khứ":
Thông qua các ứng dụng di động thông minh tích hợp công nghệ AR, khi du khách đứng tại bờ sông Hoài hoặc trước các hội quán, bạn có thể quét camera để chứng kiến cảnh tượng thương cảng Hội An thế kỷ XVII tái hiện lại ngay trước mắt: các thuyền buôn gỗ mui buồm lớn của Nhật Bản, Bồ Đào Nha đang neo đậu; cảnh tiểu thương mặc trang phục cổ xưa tấp nập bốc dỡ hàng hóa trên bến dưới thuyền.
Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 15.879, 108.326, 'Quảng Nam', '/media/heritage/pho-co-hoi-an/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Phố Cổ Hội An
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000018-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Chùa Cầu (Lai Viễn Kiều)', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu…', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.', 'artifact:pho-co-hoi-an-chua-cau', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000019-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Hội quán Phúc Kiến', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến…', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.', 'artifact:pho-co-hoi-an-hoi-quan-phuc-kien', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000020-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Nhà cổ Tân Ký', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ…', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.', 'artifact:pho-co-hoi-an-nha-co-tan-ky', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000021-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Tượng Khỉ và Chó đá', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có…', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 'artifact:pho-co-hoi-an-linh-vat-cau', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222206', 'Thành Nhà Hồ', '1. Tổng quan & Vị trí
Vị trí: Nằm trên địa phận hai xã Vĩnh Tiến và Vĩnh Long, thuộc huyện Vĩnh Lộc, tỉnh Thanh Hóa.
Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 20.077, 105.603, 'Thanh Hóa', '/media/heritage/thanh-nha-ho/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Thành Nhà Hồ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000022-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Cổng Nam (Cổng Tiền)', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao:…', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.', 'artifact:thanh-nha-ho-cong-nam', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000023-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Khối đá thạch anh', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông…', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.', 'artifact:thanh-nha-ho-khoi-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000024-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Gạch in chữ Tây Đô', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng…', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)', 'artifact:thanh-nha-ho-gach-tay-do', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000025-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Đạn đá thần cơ', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D…', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 'artifact:thanh-nha-ho-dan-da', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222207', 'văn miếu quốc tử giám', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 58 phố Quốc Tử Giám, quận Đống Đa, Hà Nội. Khu di tích được bao bọc bởi 4 tuyến phố trung tâm: Nguyễn Thái Học, Tôn Đức Thắng, Văn Miếu và Quốc Tử Giám.
Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia
Toàn bộ khuôn viên Văn Miếu - Quốc Tử Giám được bao quanh bởi tường gạch vồ kiên cố, bố cục đối xứng từng khu vực theo trục chính từ Nam ra Bắc, mô phỏng theo khuôn mẫu của Văn Miếu Khổng Tử tại Trung Quốc nhưng mang đậm bản sắc kiến trúc dân tộc Việt Nam.
A. Trục tham quan chính (5 khu vực)
Khu thứ nhất (Từ cổng chính Văn Miếu Môn đến cổng Đại Trung Môn): Không gian mở với thảm cỏ xanh, cây cổ thụ, tạo cảm giác thanh tịnh tách biệt với phố thị ồn ào.
Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.
Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 21.027, 105.835, 'Hà Nội', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: văn miếu quốc tử giám
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000026-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Khuê Văn Các', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'artifact:van-mieu-quoc-tu-giam-khue-van-cac', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000027-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', '82 bia Tiến sĩ', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai…', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia', 'artifact:van-mieu-quoc-tu-giam-bia-tien-si', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000028-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Hồ Thiên Quang', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự…', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.', 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000029-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Tượng Chu Văn An', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu…', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 'artifact:van-mieu-quoc-tu-giam-chu-van-an', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222208', 'Đại nội Huế', '1. Tổng quan & Vị trí
Vị trí: Nằm ở bờ Bắc sông Hương, thuộc địa phận kinh thành Huế.
Khái niệm: Đại Nội là cụm di tích bao gồm Hoàng Thành (nơi vua thiết triều, làm việc và các cung điện thờ tự) và Tử Cấm Thành (nơi sinh hoạt, ăn ở của vua và hoàng gia).
Tầm vóc: Đây là công trình có quy mô đồ sộ nhất trong lịch sử Việt Nam, mất gần 30 năm với hàng vạn người tham gia xây dựng và là hạt nhân của Quần thể di tích Cố đô Huế (được UNESCO công nhận là Di sản Văn hóa Thế giới từ năm 1993).
2. Lịch sử hình thành
Khởi dựng (1804): Sau khi lên ngôi và thống nhất đất nước, vua Gia Long đã khảo sát và chọn khu đất bên bờ sông Hương để xây dựng kinh thành mới. Việc xây dựng Hoàng Thành bắt đầu từ mùa hè năm 1804.
Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.
Cửu Đỉnh: Đặt trước sân Thế Miếu, gồm 9 chiếc đỉnh đồng lớn được đúc dưới thời vua Minh Mạng (1835). Mỗi chiếc đỉnh mang tên hiệu của một vị vua và được chạm khắc tinh xảo các hình ảnh biểu tượng cho giang sơn, sông núi, sản vật của Việt Nam. Đây là những bảo vật quốc gia có một không hai.
Hiển Lâm Các: Công trình kiến trúc bằng gỗ cao 3 tầng, được ví như đài kỷ niệm ghi nhớ công ơn của các bậc công thần đã có đóng góp cho triều đại.
4. Công nghệ AR & Trải nghiệm thực tế ảo tại Đại Nội
Như dòng trạng thái bạn chia sẻ ("mở khóa trải nghiệm thực tế ảo"), Đại Nội Huế hiện nay là một trong những điểm đi đầu tại Việt Nam trong việc ứng dụng công nghệ vào du lịch di sản:
Trải nghiệm AR (Augmented Reality - Thực tế tăng cường): Du khách có thể dùng điện thoại thông minh hoặc máy tính bảng quét các bảng mã tại điểm di tích để "hồi sinh" các cung điện đã bị chiến tranh phá hủy. Bạn sẽ nhìn thấy kiến trúc 3D nguyên bản của Điện Cần Chánh, Điện Kiến Trung... hiện lên ngay trên nền móng đổ nát cũ một cách sống động.
Không gian ảo VR 3D (Virtual Reality): Tại khu vực Trung tâm Phục dựng Di sản, du khách có thể đeo kính VR để trải nghiệm tour tham quan xuyên không gian, tìm hiểu về lối sống cung đình, xem các nghi lễ triều chính mô phỏng như thật.
Quét mã ngắm cổ vật: Các bảo vật quốc gia như Cửu Đỉnh hay các hiện vật trong điện thờ đều được số hóa, cho phép người xem xoay 360 độ trên thiết bị để nhìn rõ từng đường nét chạm khắc nhỏ nhất.
Đại Nội Huế không chỉ là một chuyến đi tham quan lịch sử đơn thuần, mà là sự giao thoa giữa giá trị cổ kính cốt lõi và dòng chảy công nghệ hiện đại, giúp thế hệ trẻ dễ dàng tiếp cận quá khứ một cách trực quan nhất.', 16.469, 107.579, 'Huế', '/media/heritage/dai-noi-hue/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đại nội Huế
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000030-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngọ Môn', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-ngo-mon', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000031-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngai Vàng Điện Thái Hòa', '/media/heritage/dai-noi-hue/cover.jpeg', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ…', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.', 'artifact:dai-noi-hue-ngai-vang', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000032-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Cửu Đỉnh', '/media/heritage/dai-noi-hue/cover.jpeg', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng…', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)', 'artifact:dai-noi-hue-cuu-dinh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000033-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Lầu Ngũ Phụng', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-lau-ngu-phung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222209', 'Đền Hùng Vương', '1. Tổng quan & Vị trí
Vị trí: Nằm trên núi Nghĩa Lĩnh (núi Hùng), thuộc xã Hy Cương, thành phố Việt Trì, tỉnh Phú Thọ, cách thủ đô Hà Nội khoảng 85km về phía Tây Bắc.
Tầm vóc: Đây là khu di tích lịch sử đặc biệt cấp quốc gia, trung tâm thờ tự tổ tiên của người Việt. Năm 2012, Tín ngưỡng thờ cúng Hùng Vương ở Phú Thọ đã được UNESCO công nhận là Di sản văn hóa phi vật thể đại diện của nhân loại.
Cảnh quan: Núi Hùng cao khoảng 175m, xung quanh là hệ thống đồi gò trùng điệp (truyền thuyết gọi là 99 con voi quay đầu về chầu nghĩa quân). Không gian nơi đây quanh năm mây mờ bao phủ, cây cối cổ thụ xanh tươi, mang đậm màu sắc huyền thoại, linh thiêng.
2. Lịch sử hình thành & Giá trị cội nguồn
Thời kỳ lập nước: Theo truyền thuyết, nơi đây chính là trung tâm kinh đô Phong Châu của nước Văn Lang cổ đại. Các vị vua Hùng đã chọn đỉnh núi Nghĩa Lĩnh cao nhất vùng để làm nơi tiến hành các nghi lễ tín ngưỡng bản địa: tế trời đất, tế thần lúa, cầu mong mưa thuận gió hòa, mùa màng bội thu.
Xây dựng qua các triều đại: Hệ thống kiến trúc lăng tẩm, đền đài được xây dựng từng bước và trùng tu lớn qua các thời kỳ Lý, Trần, Lê, Nguyễn. Công trình hiện nay phần lớn mang phong cách kiến trúc thời hậu Lê và thời Nguyễn.
Ngày Quốc Giỗ: Hằng năm, vào ngày mồng 10 tháng Ba âm lịch, hàng triệu người con đất Việt từ khắp mọi miền tổ quốc và kiều bào nước ngoài lại hướng về Đền Hùng để làm lễ dâng hương, thể hiện truyền thống đạo lý "Uống nước nhớ nguồn".
3. Cấu trúc không gian & Hệ thống đền đài từ chân lên đỉnh núi
Hành trình tham quan Đền Hùng là một trải nghiệm leo núi thiêng qua từng bậc đá cổ kính, tương ứng với các tầng di tích:
Đại Môn (Cổng đền): Được xây dựng vào năm 1917 dưới thời vua Khải Định. Cổng có kiến trúc kiểu vòm cuốn nguy nga, trên đỉnh đắp phù điêu hai chiến sĩ cầm chùy canh giữ, chính giữa khắc bốn chữ Hán lớn: Cao sơn cảnh hành (ý nói đức lớn của các vua Hùng cao như núi, đường lối của vua như đường lớn để mọi người noi theo).
Đền Hạ: Nằm ở tầng núi thấp nhất. Theo truyền thuyết, đây là nơi mẹ Âu Cơ đã hạ sinh ra bọc trăm trứng, sau đó nở thành một trăm người con trai, cội nguồn của hai tiếng "đồng bào". Ngay cạnh đền là Chùa Thiên Quang cổ kính.
Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:
Trải nghiệm AR "Hồi sinh không gian huyền thoại":
Khi du khách đứng tại Đền Hạ, việc sử dụng thiết bị quét mã AR sẽ tái hiện lại sống động bằng hoạt họa 3D câu chuyện bọc trăm trứng nở thành trăm người con, hay hình ảnh đoàn người lên núi tế trời đất thời cổ đại.
Tại Đền Trung, AR giúp mô phỏng lại hình ảnh Lang Liêu dâng chiếc bánh chưng, bánh giầy vuông tròn tượng trưng cho trời và đất lên vua cha, giúp bài học lịch sử trở nên thu hút, dễ hiểu đối với các bạn trẻ.
Bản đồ số hóa & Thuyết minh tự động: Hệ thống bản đồ 3D hiển thị chi tiết số lượng bậc đá, tọa độ các đền từ chân núi lên đỉnh núi Nghĩa Lĩnh. Du khách có thể dùng điện thoại tự quét mã tại mỗi điểm để nghe trợ lý AI thuyết minh tường tận về các truyền thuyết, ý nghĩa của từng câu đối, hoành phi cổ.
Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 21.383, 105.298, 'Phú Thọ', '/media/heritage/den-hung-vuong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đền Hùng Vương
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000034-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Trống đồng Đông Sơn', '/media/heritage/den-hung-vuong/cover.webp', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc…', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 'artifact:den-hung-vuong-trong-dong', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000035-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Bánh chưng, bánh giầy', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên…', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-banh-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000036-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Cột đá thề An Dương Vương', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng…', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-cot-da-the', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000037-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Giếng Ngọc', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm"…', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-gieng-ngoc', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('11111111-1111-1111-1111-111111111111', 'Địa đạo Củ Chi', '1. Tổng quan & Vị trí
Vị trí: Thuộc huyện Củ Chi, TP. Hồ Chí Minh, cách trung tâm thành phố khoảng 70km về phía Tây Bắc. Khu di tích hiện nay được bảo tồn ở hai địa điểm chính: Địa đạo Bến Dược (Căn cứ Khu ủy và Bộ Tư lệnh Quân khu Sài Gòn - Chợ Lớn - Gia Định) và Địa đạo Bến Đình (Căn cứ Huyện ủy Củ Chi).
Tầm vóc: Đây là một hệ thống phòng thủ, căn cứ quân sự trong lòng đất vô cùng đồ sộ. Địa đạo Củ Chi đã được xếp hạng Di sản cấp quốc gia đặc biệt và đang trong quá trình hoàn thiện hồ sơ đệ trình UNESCO công nhận là Di sản Thế giới.
Danh hiệu: Nơi đây được mệnh danh là "Mảnh đất thép thành đồng" nhờ khả năng đứng vững trước hàng triệu tấn bom đạn của kẻ thù.
2. Lịch sử hình thành: Kỳ tích từ những chiếc cuốc thô sơ
Khởi nguồn (1946 - 1948): Hệ thống địa đạo bắt đầu được đào từ thời kỳ kháng chiến chống Pháp. Ban đầu, các xã chỉ đào những hầm bí mật đơn lẻ để ẩn nấp cán bộ và cất giấu tài liệu.
Mở rộng thành "Mạng nhện" (1961 - 1967): Bước sang cuộc kháng chiến chống Mỹ, trước sự càn quét khốc liệt của địch, hệ thống này được phát triển, nối liền các xã với nhau tạo thành một mạng lưới liên hoàn trong lòng đất. Quân và dân Củ Chi đã dùng những công cụ thô sơ như cuốc, xẻng và sọt tre để kiên trì đào xuyên lòng đất ngày đêm, tạo nên một "thành phố ngầm" kỳ vĩ.
3. Kiến trúc lòng đất độc đáo "Bất khả xâm phạm"
Hệ thống địa đạo Củ Chi có tổng chiều dài toàn tuyến lên đến hơn 250 km, chạy ngoằn ngoèo trong lòng đất đất sét pha đá sỏi (loại đất chịu lực tốt, càng ra gió càng cứng, không bị sập).
Cấu trúc 3 tầng biệt lập:
Tầng 1 (Cách mặt đất 3m): Chịu được đạn pháo và xe tăng, xe bọc thép cán qua.
Tầng 2 (Cách mặt đất 6m): Chịu được các loại bom cỡ nhỏ.
Tầng 3 (Cách mặt đất từ 8 - 12m): Tầng sâu nhất, cực kỳ an toàn, bất chấp cả bom cỡ lớn.
Hệ thống phòng ốc liên hoàn: Bên trong địa đạo không chỉ là đường đi lối lại chật hẹp mà còn có đầy đủ các không gian sinh hoạt: phòng họp của Bộ tư lệnh, hầm trú ẩn cho người già và trẻ em, công xưởng chế tạo vũ khí, nhà thương (bệnh viện dã chiến), nhà bếp, giếng nước và kho chứa lương thực.
Bếp Hoàng Cầm: Một phát minh vĩ đại của thời chiến được áp dụng triệt để tại đây. Bếp nấu ăn dưới lòng đất nhưng hệ thống dẫn khói được tản ra đi là là trên mặt đất qua các rãnh nhỏ cách xa bếp, giúp khói tan rã hoàn toàn vào sương sớm, khiến máy bay địch không thể phát hiện ra vị trí trú ẩn.
Lỗ thông hơi và Hệ thống hầm chông: Các lỗ thông hơi được ngụy trang khéo léo thành những tổ mối, gốc cây để lấy dưỡng khí. Bao quanh các lối vào địa đạo là mạng lưới mìn tự tạo và bẫy chông tre, chông sắt dày đặc nhằm ngăn chặn sự xâm nhập của lính thám báo.
4. Ứng dụng AR và Trải nghiệm số hóa tại Địa đạo Củ Chi
Vì địa đạo nằm sâu trong lòng đất, không gian tối và chật hẹp khiến việc tham quan thực tế đôi khi gặp hạn chế đối với một số du khách (người sợ không gian kín hoặc hạn chế về sức khỏe). Công nghệ AR và số hóa đã giải quyết xuất sắc bài toán này:
Mô phỏng "Cắt lớp lòng đất" bằng AR:
Khi đứng trên mặt đất, du khách chỉ cần dùng điện thoại hoặc kính thông minh quét không gian, ứng dụng AR sẽ hiển thị một sơ đồ 3D "xuyên thấu" mặt đất. Bạn sẽ nhìn thấy rõ ràng cấu trúc 3 tầng của địa đạo đang chạy ngay dưới chân mình, thấy cách các chiến sĩ di chuyển, hội họp và sinh hoạt bên dưới một cách trực quan.
AR cũng mô phỏng nguyên lý hoạt động giấu khói thần kỳ của Bếp Hoàng Cầm hay cách hoạt động của các loại bẫy chông ngụy trang một cách sống động mà không cần phải chạm vào hiện vật thật.
Tour tham quan ảo 360° dưới lòng đất: Đối với những du khách không thể xuống lòng đất, hệ thống quét hình ảnh 360 độ độ phân giải cao kết hợp với âm thanh giả lập (tiếng bom đạn, tiếng cuốc đất, tiếng thì thầm thời chiến) giúp họ có trải nghiệm như đang thực sự len lỏi qua từng vách đất của hầm chỉ huy Bến Dược.
Số hóa vũ khí tự chế: Du khách có thể tương tác với các mô hình 3D của các loại vũ khí tự chế độc đáo của du kích Củ Chi (như mìn gạt làm từ vỏ bom lép của địch, chông bập bênh...) trên màn hình cảm ứng, tìm hiểu cơ chế hoạt động lịch sử của chúng.
Địa đạo Củ Chi là một trường ca về lòng yêu nước và sự sáng tạo không giới hạn của con người. Sự hỗ trợ của công nghệ AR giống như một chiếc kính xuyên không, giúp chúng ta nhìn thấu lòng đất để thấu hiểu và tri ân một thời kỳ gian khổ nhưng vô cùng vẻ vang của thế hệ đi trước.', 11.143, 106.461, 'TP.HCM', '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

COMMIT;

-- END seed: 2026-06-19_heritage_sites_from_dataset.sql

-- ============================================================================
-- BEGIN seed: 2026-06-20_fix_den_hung_vuong_name.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-20_fix_den_hung_vuong_name.sql
-- ============================================================================
-- Fix typo: Đền Hùng Vướng → Đền Hùng Vương
UPDATE locations
SET name = 'Đền Hùng Vương'
WHERE id = '22222222-2222-2222-2222-222222222209'
   OR name ILIKE '%Hùng Vướng%';

-- END seed: 2026-06-20_fix_den_hung_vuong_name.sql

-- ============================================================================
-- BEGIN seed: 2026-06-21_heritage_quests_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-21_heritage_quests_seed.sql
-- ============================================================================
-- Heritage quests: one main quest per location (content from dataset / AI docs)
BEGIN;

-- Cu Chi — keep canonical quest id; onsite check-in completes after online steps
INSERT INTO quests (
  id, location_id, title, description, story, points_reward, required_order,
  step_discovery_keys, completion_trigger, steps_total, cover_image
)
VALUES (
  '33333333-3333-3333-3333-333333333333',
  '11111111-1111-1111-1111-111111111111',
  'Hành trình dưới lòng đất',
  'Khám phá Bếp Hoàng Cầm, Phòng họp và Time Portal 1948 — sau đó check-in tại khu di tích Củ Chi.',
  'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.',
  100, 1,
  'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223,era:1948',
  'checkin', 4,
  '/media/heritage/dia-dao-cu-chi/cover.jpeg'
)
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  story = EXCLUDED.story,
  points_reward = EXCLUDED.points_reward,
  step_discovery_keys = EXCLUDED.step_discovery_keys,
  completion_trigger = EXCLUDED.completion_trigger,
  steps_total = EXCLUDED.steps_total,
  cover_image = EXCLUDED.cover_image;

INSERT INTO quests (id, location_id, title, description, story, points_reward, required_order, step_discovery_keys, completion_trigger, steps_total, cover_image)
VALUES
(
  '44444444-4444-4444-4444-444444444401',
  '22222222-2222-2222-2222-222222222201',
  'Bước chân ra thế giới',
  'Chiêm ngưỡng rồng trên mái → Time Portal bến sông → vali mây.',
  'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Nhìn rồng trên mái, so sánh ảnh xưa–nay, rồi khám phá vali mây — không cần đọc hồ sơ dài.',
  120, 1,
  'artifact:ben-nha-rong-rong-mai,era:2026,artifact:ben-nha-rong-vali-may',
  'discovery', 3,
  '/media/heritage/ben-nha-rong/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444402',
  '22222222-2222-2222-2222-222222222202',
  'Ngọn tháp bên sông Hương',
  'Tháp Phước Duyên → Time Portal sông Hương → bia đá trên lưng rùa.',
  'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chiêm ngưỡng tháp, xem Time Portal, rồi mở bia đá — trải nghiệm bằng mắt.',
  100, 1,
  'artifact:chua-thien-mu-thap-phuoc-duyen,era:2026,artifact:chua-thien-mu-bia-da',
  'discovery', 3,
  '/media/heritage/chua-thien-mu/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444403',
  '22222222-2222-2222-2222-222222222203',
  'Dấu ấn kinh đô đầu tiên',
  'Long Sàng đá → Time Portal kinh đô → đền Lê Đại Hành.',
  '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Khám phá Long Sàng, so sánh ảnh xưa–nay, rồi mở đền Lê Đại Hành.',
  120, 1,
  'artifact:co-do-hoa-lu-long-sang,era:2026,artifact:co-do-hoa-lu-den-le-dai-hanh',
  'discovery', 3,
  '/media/heritage/co-do-hoa-lu/cover.jpeg'
),
(
  '44444444-4444-4444-4444-444444444404',
  '22222222-2222-2222-2222-222222222204',
  'Dấu ấn Hoàng Thành',
  'Móng cột cổ → Time Portal Thăng Long → ngói men xanh.',
  'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp hiện vật và Time Portal như một game khảo cổ.',
  150, 1,
  'artifact:hoang-thanh-thang-long-cot-co,era:2026,artifact:hoang-thanh-thang-long-ngoi-phuong',
  'discovery', 3,
  '/media/heritage/hoang-thanh-thang-long/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444405',
  '22222222-2222-2222-2222-222222222205',
  'Bí ẩn Chùa Cầu',
  'Chùa Cầu → Time Portal phố cổ → nhà Tấn Ký.',
  'Hội An không chỉ đẹp trong ảnh — mỗi mái ngói là một chuyến hàng từ phương xa. Nhìn Chùa Cầu, so sánh phố cổ, rồi mở nhà Tấn Ký.',
  100, 1,
  'artifact:pho-co-hoi-an-chua-cau,era:2026,artifact:pho-co-hoi-an-nha-co-tan-ky',
  'discovery', 3,
  '/media/heritage/pho-co-hoi-an/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444406',
  '22222222-2222-2222-2222-222222222206',
  'Cổ thành đá Tây Đô',
  'Cổng Nam → Time Portal Tây Đô → gạch in chữ.',
  'Thành không vữa nhưng đứng vững 600 năm — khám phá cổng Nam, Time Portal, rồi gạch in chữ Tây Đô.',
  120, 1,
  'artifact:thanh-nha-ho-cong-nam,era:2026,artifact:thanh-nha-ho-gach-tay-do',
  'discovery', 3,
  '/media/heritage/thanh-nha-ho/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444407',
  '22222222-2222-2222-2222-222222222207',
  'Đường đến khoa bảng',
  'Khuê Văn Các → Time Portal Văn Miếu → Hồ Thiên Quang.',
  'Trở thành môn sinh Quốc Tử: chiêm ngưỡng Khuê Văn Các, xem Time Portal, rồi mở Hồ Thiên Quang.',
  100, 1,
  'artifact:van-mieu-quoc-tu-giam-khue-van-cac,era:2026,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  'discovery', 3,
  '/media/heritage/van-mieu-quoc-tu-giam/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444408',
  '22222222-2222-2222-2222-222222222208',
  'Bí ẩn Đại Nội',
  'Ngọ Môn → Time Portal Đại Nội → Cửu Đỉnh.',
  'Cung đình Huế im lặng nhưng đầy quyền lực. Chiêm ngưỡng Ngọ Môn, so sánh kinh thành, rồi mở Cửu Đỉnh.',
  150, 1,
  'artifact:dai-noi-hue-ngo-mon,era:2026,artifact:dai-noi-hue-cuu-dinh',
  'discovery', 3,
  '/media/heritage/dai-noi-hue/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444409',
  '22222222-2222-2222-2222-222222222209',
  'Về Đất Tổ',
  'Trống đồng → Time Portal Nghĩa Lĩnh → cột đá thế.',
  'Về cội nguồn không cần vé máy bay — chiêm ngưỡng trống đồng, xem Time Portal, rồi mở cột đá thế.',
  120, 1,
  'artifact:den-hung-vuong-trong-dong,era:2026,artifact:den-hung-vuong-cot-da-the',
  'discovery', 3,
  '/media/heritage/den-hung-vuong/cover.webp'
)
ON CONFLICT (id) DO UPDATE SET
  location_id = EXCLUDED.location_id,
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  story = EXCLUDED.story,
  points_reward = EXCLUDED.points_reward,
  step_discovery_keys = EXCLUDED.step_discovery_keys,
  completion_trigger = EXCLUDED.completion_trigger,
  steps_total = EXCLUDED.steps_total,
  cover_image = EXCLUDED.cover_image;

COMMIT;

-- END seed: 2026-06-21_heritage_quests_seed.sql

-- ============================================================================
-- BEGIN seed: 2026-06-22_fix_quest_progress_current_step.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-22_fix_quest_progress_current_step.sql
-- ============================================================================
-- Fix user_quest_progress rows missing current_step (caused start quest 500)
UPDATE user_quest_progress
SET current_step = 0
WHERE current_step IS NULL;

-- END seed: 2026-06-22_fix_quest_progress_current_step.sql

-- ============================================================================
-- BEGIN seed: 2026-06-23_quest_mission_keys.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-23_quest_mission_keys.sql
-- ============================================================================
-- Quest mission keys: briefing → dialogue → reveal (decoupled from artifact pokédex)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'quest:444444444401:brief,quest:444444444401:dialogue,quest:444444444401:reveal',
  description = 'Ba chương: đọc hồ sơ Bến Nhà Rồng → đối thoại với hướng dẫn → giải mã manh mối tàu Amiral.',
  story = 'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Bạn là nhà khảo cổ thời gian — hãy khôi phục câu chuyện trước khi manh mối biến mất.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444402:brief,quest:444444444402:dialogue,quest:444444444402:reveal',
  description = 'Ba chương bên sông Hương: truyền thuyết áo đỏ → đối thoại → giải mã tháp và bia.',
  story = 'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chỉ người kiên nhẫn đọc, hỏi và giải mã mới nghe hết câu chuyện.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444403:brief,quest:444444444403:dialogue,quest:444444444403:reveal',
  description = 'Ba chương kinh đô Hoa Lư: địa thế → trận Bạch Đằng → bảo vật đá.',
  story = '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Nhiệm vụ của bạn: chứng minh mình xứng đáng làm sứ giả kinh đô.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444404:brief,quest:444444444404:dialogue,quest:444444444404:reveal',
  description = 'Ba chương Thăng Long: tầng lịch sử → đối thoại → khai quật số.',
  story = 'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp như một game khảo cổ.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444405:brief,quest:444444444405:dialogue,quest:444444444405:reveal',
  description = 'Ba chương phố cổ: cảng quốc tế → Chùa Cầu → khảm sành Phúc Kiến.',
  story = 'Hội An không chỉ đẹp trong ảnh — mỗi mái ngói là một chuyến hàng từ phương xa.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444406:brief,quest:444444444406:dialogue,quest:444444444406:reveal',
  description = 'Ba chương Tây Đô: thành đá → chiến lược → gạch in chữ.',
  story = 'Thành không vữa nhưng đứng vững 600 năm — bạn có giải được bí mật kỹ thuật?'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444407:brief,quest:444444444407:dialogue,quest:444444444407:reveal',
  description = 'Ba chương Văn Miếu: khoa bảng → Khuê Văn Các → 82 bia Tiến sĩ.',
  story = 'Trở thành môn sinh Quốc Tử: đọc, hỏi, rồi khắc tên mình vào dòng khoa bảng — bằng hành động.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444408:brief,quest:444444444408:dialogue,quest:444444444408:reveal',
  description = 'Ba chương Đại Nội: Ngọ Môn → điện Thái Hòa → Cửu Đỉnh.',
  story = 'Cung đình Huế im lặng nhưng đầy quyền lực. Chỉ sứ giả mới đi hết trục thiên tử.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444409:brief,quest:444444444409:dialogue,quest:444444444409:reveal',
  description = 'Ba chương Đền Hùng: giỗ Tổ → Lạc Hồng → trống đồng.',
  story = 'Về cội nguồn không cần vé máy bay — cần lòng tò mò và ba chương kiên trì.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;

-- END seed: 2026-06-23_quest_mission_keys.sql

-- ============================================================================
-- BEGIN seed: 2026-06-24_quest_visual_artifact_keys.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-24_quest_visual_artifact_keys.sql
-- ============================================================================
-- Quest visual flow: artifact discovery keys (see objects, not read text)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'artifact:ben-nha-rong-rong-mai,artifact:ben-nha-rong-tau-amiral,artifact:ben-nha-rong-vali-may',
  description = 'Chiêm ngưỡng 3 hiện vật Bến Nhà Rồng — rồng trên mái, tàu Amiral, vali mây.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'artifact:chua-thien-mu-thap-phuoc-duyen,artifact:chua-thien-mu-dai-hong-chung,artifact:chua-thien-mu-bia-da',
  description = 'Xem 3 hiện vật Chùa Thiên Mụ — tháp, chuông và bia đá.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'artifact:co-do-hoa-lu-long-sang,artifact:co-do-hoa-lu-cot-kinh,artifact:co-do-hoa-lu-den-le-dai-hanh',
  description = 'Khám phá Long Sàng, cột kinh và đền Lê Đại Hành qua hiện vật số.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'artifact:hoang-thanh-thang-long-cot-co,artifact:hoang-thanh-thang-long-doi-rong-da,artifact:hoang-thanh-thang-long-ngoi-phuong',
  description = 'Khai quật số — móng cột, rồng đá và ngói men xanh Hoàng Thành.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'artifact:pho-co-hoi-an-chua-cau,artifact:pho-co-hoi-an-hoi-quan-phuc-kien,artifact:pho-co-hoi-an-nha-co-tan-ky',
  description = 'Nhìn Chùa Cầu, hội quán Phúc Kiến và nhà Tấn Ký qua tủ hiện vật.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'artifact:thanh-nha-ho-cong-nam,artifact:thanh-nha-ho-khoi-da,artifact:thanh-nha-ho-gach-tay-do',
  description = 'Thành đá Tây Đô — cổng Nam, khối đá và gạch in chữ.'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'artifact:van-mieu-quoc-tu-giam-khue-van-cac,artifact:van-mieu-quoc-tu-giam-bia-tien-si,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  description = 'Khuê Văn Các, 82 bia Tiến sĩ và Hồ Thiên Quang — trải nghiệm bằng mắt.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'artifact:dai-noi-hue-ngo-mon,artifact:dai-noi-hue-ngai-vang,artifact:dai-noi-hue-cuu-dinh',
  description = 'Ngọ Môn, ngai vàng và Cửu Đỉnh — bảo vật triều Nguyễn trong tủ sưu tập.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'artifact:den-hung-vuong-trong-dong,artifact:den-hung-vuong-banh-chung,artifact:den-hung-vuong-cot-da-the',
  description = 'Trống đồng, bánh chưng và cột đá — cội nguồn Lạc Hồng qua hiện vật.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;

-- END seed: 2026-06-24_quest_visual_artifact_keys.sql

-- ============================================================================
-- BEGIN seed: 2026-06-25_quest_mixed_visual_pipeline.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-25_quest_mixed_visual_pipeline.sql
-- ============================================================================
-- P1 mixed visual pipeline: artifact → era:2026 portal → artifact (9 heritage quests)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'artifact:ben-nha-rong-rong-mai,era:2026,artifact:ben-nha-rong-vali-may',
  description = 'Chiêm ngưỡng rồng trên mái → Time Portal bến sông → vali mây.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'artifact:chua-thien-mu-thap-phuoc-duyen,era:2026,artifact:chua-thien-mu-bia-da',
  description = 'Tháp Phước Duyên → Time Portal sông Hương → bia đá trên lưng rùa.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'artifact:co-do-hoa-lu-long-sang,era:2026,artifact:co-do-hoa-lu-den-le-dai-hanh',
  description = 'Long Sàng đá → Time Portal kinh đô → đền Lê Đại Hành.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'artifact:hoang-thanh-thang-long-cot-co,era:2026,artifact:hoang-thanh-thang-long-ngoi-phuong',
  description = 'Móng cột cổ → Time Portal Thăng Long → ngói men xanh.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'artifact:pho-co-hoi-an-chua-cau,era:2026,artifact:pho-co-hoi-an-nha-co-tan-ky',
  description = 'Chùa Cầu → Time Portal phố cổ → nhà Tấn Ký.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'artifact:thanh-nha-ho-cong-nam,era:2026,artifact:thanh-nha-ho-gach-tay-do',
  description = 'Cổng Nam → Time Portal Tây Đô → gạch in chữ.'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'artifact:van-mieu-quoc-tu-giam-khue-van-cac,era:2026,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  description = 'Khuê Văn Các → Time Portal Văn Miếu → Hồ Thiên Quang.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'artifact:dai-noi-hue-ngo-mon,era:2026,artifact:dai-noi-hue-cuu-dinh',
  description = 'Ngọ Môn → Time Portal Đại Nội → Cửu Đỉnh.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'artifact:den-hung-vuong-trong-dong,era:2026,artifact:den-hung-vuong-cot-da-the',
  description = 'Trống đồng → Time Portal Nghĩa Lĩnh → cột đá thế.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;

-- END seed: 2026-06-25_quest_mixed_visual_pipeline.sql

-- ============================================================================
-- BEGIN seed: 2026-06-26_heritage_quest_discovery_points.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-26_heritage_quest_discovery_points.sql
-- ============================================================================
-- P1 support data: discovery_points + photo_pairs for 9 heritage quest pipelines
BEGIN;

-- Quest step keys must exist in discovery_points (DiscoveryServiceImpl + SeedConsistencyValidator)
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
-- Bến Nhà Rồng
('d0000002-0000-4000-8000-000000000201', '22222222-2222-2222-2222-222222222201', 'Hai con rồng trên mái', 42.00, 38.00, 'artifact:ben-nha-rong-rong-mai', 1),
('d0000002-0000-4000-8000-000000000202', '22222222-2222-2222-2222-222222222201', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000203', '22222222-2222-2222-2222-222222222201', 'Chiếc vali mây', 58.00, 42.00, 'artifact:ben-nha-rong-vali-may', 3),
-- Chùa Thiên Mụ
('d0000002-0000-4000-8000-000000000211', '22222222-2222-2222-2222-222222222202', 'Tháp Phước Duyên', 42.00, 38.00, 'artifact:chua-thien-mu-thap-phuoc-duyen', 1),
('d0000002-0000-4000-8000-000000000212', '22222222-2222-2222-2222-222222222202', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000213', '22222222-2222-2222-2222-222222222202', 'Bia đá trên lưng rùa', 58.00, 42.00, 'artifact:chua-thien-mu-bia-da', 3),
-- Cố đô Hoa Lư
('d0000002-0000-4000-8000-000000000221', '22222222-2222-2222-2222-222222222203', 'Long Sàng đá', 42.00, 38.00, 'artifact:co-do-hoa-lu-long-sang', 1),
('d0000002-0000-4000-8000-000000000222', '22222222-2222-2222-2222-222222222203', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000223', '22222222-2222-2222-2222-222222222203', 'Đền Lê Đại Hành', 58.00, 42.00, 'artifact:co-do-hoa-lu-den-le-dai-hanh', 3),
-- Hoàng Thành Thăng Long
('d0000002-0000-4000-8000-000000000231', '22222222-2222-2222-2222-222222222204', 'Móng cột cổ', 42.00, 38.00, 'artifact:hoang-thanh-thang-long-cot-co', 1),
('d0000002-0000-4000-8000-000000000232', '22222222-2222-2222-2222-222222222204', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000233', '22222222-2222-2222-2222-222222222204', 'Ngói men xanh', 58.00, 42.00, 'artifact:hoang-thanh-thang-long-ngoi-phuong', 3),
-- Phố cổ Hội An
('d0000002-0000-4000-8000-000000000241', '22222222-2222-2222-2222-222222222205', 'Chùa Cầu', 42.00, 38.00, 'artifact:pho-co-hoi-an-chua-cau', 1),
('d0000002-0000-4000-8000-000000000242', '22222222-2222-2222-2222-222222222205', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000243', '22222222-2222-2222-2222-222222222205', 'Nhà cổ Tấn Ký', 58.00, 42.00, 'artifact:pho-co-hoi-an-nha-co-tan-ky', 3),
-- Thành Nhà Hồ
('d0000002-0000-4000-8000-000000000251', '22222222-2222-2222-2222-222222222206', 'Cổng Nam', 42.00, 38.00, 'artifact:thanh-nha-ho-cong-nam', 1),
('d0000002-0000-4000-8000-000000000252', '22222222-2222-2222-2222-222222222206', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000253', '22222222-2222-2222-2222-222222222206', 'Gạch in chữ Tây Đô', 58.00, 42.00, 'artifact:thanh-nha-ho-gach-tay-do', 3),
-- Văn Miếu – Quốc Tử Giám
('d0000002-0000-4000-8000-000000000261', '22222222-2222-2222-2222-222222222207', 'Khuê Văn Các', 42.00, 38.00, 'artifact:van-mieu-quoc-tu-giam-khue-van-cac', 1),
('d0000002-0000-4000-8000-000000000262', '22222222-2222-2222-2222-222222222207', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000263', '22222222-2222-2222-2222-222222222207', 'Hồ Thiên Quang', 58.00, 42.00, 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 3),
-- Đại Nội Huế
('d0000002-0000-4000-8000-000000000271', '22222222-2222-2222-2222-222222222208', 'Ngọ Môn', 42.00, 38.00, 'artifact:dai-noi-hue-ngo-mon', 1),
('d0000002-0000-4000-8000-000000000272', '22222222-2222-2222-2222-222222222208', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000273', '22222222-2222-2222-2222-222222222208', 'Cửu Đỉnh', 58.00, 42.00, 'artifact:dai-noi-hue-cuu-dinh', 3),
-- Đền Hùng
('d0000002-0000-4000-8000-000000000281', '22222222-2222-2222-2222-222222222209', 'Trống đồng Đông Sơn', 42.00, 38.00, 'artifact:den-hung-vuong-trong-dong', 1),
('d0000002-0000-4000-8000-000000000282', '22222222-2222-2222-2222-222222222209', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000283', '22222222-2222-2222-2222-222222222209', 'Cột đá thế', 58.00, 42.00, 'artifact:den-hung-vuong-cot-da-the', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name, map_x_pct = EXCLUDED.map_x_pct, map_y_pct = EXCLUDED.map_y_pct, sort_order = EXCLUDED.sort_order;

-- Time Portal fallback: one photo pair per heritage site (Then/Now slider)
DELETE FROM photo_pairs
WHERE location_id IN (
  '22222222-2222-2222-2222-222222222201',
  '22222222-2222-2222-2222-222222222202',
  '22222222-2222-2222-2222-222222222203',
  '22222222-2222-2222-2222-222222222204',
  '22222222-2222-2222-2222-222222222205',
  '22222222-2222-2222-2222-222222222206',
  '22222222-2222-2222-2222-222222222207',
  '22222222-2222-2222-2222-222222222208',
  '22222222-2222-2222-2222-222222222209'
);

INSERT INTO photo_pairs (location_id, historical_image, current_image, year, caption, sort_order) VALUES
('22222222-2222-2222-2222-222222222201', '/media/heritage/ben-nha-rong/cover.webp', '/media/heritage/ben-nha-rong/cover.webp', 1911, 'Bến Nhà Rồng — xưa và nay', 1),
('22222222-2222-2222-2222-222222222202', '/media/heritage/chua-thien-mu/cover.webp', '/media/heritage/chua-thien-mu/cover.webp', 1844, 'Chùa Thiên Mụ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222203', '/media/heritage/co-do-hoa-lu/cover.jpeg', '/media/heritage/co-do-hoa-lu/cover.jpeg', 968, 'Cố đô Hoa Lư — xưa và nay', 1),
('22222222-2222-2222-2222-222222222204', '/media/heritage/hoang-thanh-thang-long/cover.webp', '/media/heritage/hoang-thanh-thang-long/cover.webp', 1010, 'Hoàng Thành Thăng Long — xưa và nay', 1),
('22222222-2222-2222-2222-222222222205', '/media/heritage/pho-co-hoi-an/cover.webp', '/media/heritage/pho-co-hoi-an/cover.webp', 1600, 'Phố cổ Hội An — xưa và nay', 1),
('22222222-2222-2222-2222-222222222206', '/media/heritage/thanh-nha-ho/cover.webp', '/media/heritage/thanh-nha-ho/cover.webp', 1397, 'Thành Nhà Hồ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222207', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp', 1070, 'Văn Miếu — xưa và nay', 1),
('22222222-2222-2222-2222-222222222208', '/media/heritage/dai-noi-hue/cover.webp', '/media/heritage/dai-noi-hue/cover.webp', 1802, 'Đại Nội Huế — xưa và nay', 1),
('22222222-2222-2222-2222-222222222209', '/media/heritage/den-hung-vuong/cover.webp', '/media/heritage/den-hung-vuong/cover.webp', 2879, 'Đền Hùng — xưa và nay', 1);

COMMIT;

-- END seed: 2026-06-26_heritage_quest_discovery_points.sql

-- ============================================================================
-- BEGIN seed: 2026-06-27_heritage_p2_checkin_bonus.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-27_heritage_p2_checkin_bonus.sql
-- ============================================================================
-- P2: heritage onsite check-in bonus XP + per-site "Đã đến nơi" badges + real Then/Now photo_pairs
BEGIN;

ALTER TABLE badges ADD COLUMN IF NOT EXISTS location_id UUID REFERENCES locations(id) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS user_heritage_onsite_bonus (
    user_id     UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    xp_awarded  INT NOT NULL DEFAULT 25,
    awarded_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, location_id)
);

-- Per-site onsite seal badges (condition: heritage_onsite + location_id)
INSERT INTO badges (id, name, description, icon_url, condition_type, condition_value, location_id) VALUES
('e0000001-0000-4000-8000-000000000201', 'Đã đến nơi · Bến Nhà Rồng', 'Check-in tại Bến Nhà Rồng sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222201'),
('e0000001-0000-4000-8000-000000000202', 'Đã đến nơi · Chùa Thiên Mụ', 'Check-in tại Chùa Thiên Mụ sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222202'),
('e0000001-0000-4000-8000-000000000203', 'Đã đến nơi · Cố đô Hoa Lư', 'Check-in tại Cố đô Hoa Lư sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222203'),
('e0000001-0000-4000-8000-000000000204', 'Đã đến nơi · Hoàng Thành', 'Check-in tại Hoàng Thành Thăng Long sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222204'),
('e0000001-0000-4000-8000-000000000205', 'Đã đến nơi · Hội An', 'Check-in tại Phố cổ Hội An sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222205'),
('e0000001-0000-4000-8000-000000000206', 'Đã đến nơi · Thành Nhà Hồ', 'Check-in tại Thành Nhà Hồ sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222206'),
('e0000001-0000-4000-8000-000000000207', 'Đã đến nơi · Văn Miếu', 'Check-in tại Văn Miếu sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222207'),
('e0000001-0000-4000-8000-000000000208', 'Đã đến nơi · Đại Nội Huế', 'Check-in tại Đại Nội Huế sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222208'),
('e0000001-0000-4000-8000-000000000209', 'Đã đến nơi · Đền Hùng', 'Check-in tại Đền Hùng sau khi hoàn tất nhiệm vụ online', 'https://placehold.co/200?text=Onsite', 'heritage_onsite', 1, '22222222-2222-2222-2222-222222222209')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  icon_url = EXCLUDED.icon_url,
  condition_type = EXCLUDED.condition_type,
  condition_value = EXCLUDED.condition_value,
  location_id = EXCLUDED.location_id;

-- Real Then/Now pairs (distinct historical vs present imagery per site)
DELETE FROM photo_pairs
WHERE location_id IN (
  '22222222-2222-2222-2222-222222222201',
  '22222222-2222-2222-2222-222222222202',
  '22222222-2222-2222-2222-222222222203',
  '22222222-2222-2222-2222-222222222204',
  '22222222-2222-2222-2222-222222222205',
  '22222222-2222-2222-2222-222222222206',
  '22222222-2222-2222-2222-222222222207',
  '22222222-2222-2222-2222-222222222208',
  '22222222-2222-2222-2222-222222222209'
);

INSERT INTO photo_pairs (location_id, historical_image, current_image, year, caption, sort_order) VALUES
('22222222-2222-2222-2222-222222222201', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAjLWg45sHfWuxzwOXD11fsIp1wol7YkpA93fnuNpVqzElVB4cq0xhaEVr6i-os7ogOT7f92CtQGQ7gdFjv8LNpC2ytFZGSczSzVq1FaWjLK3eYseKyjkyfL35EuR1sAf_s58Hj5HuOAdp6-tnvOIFvmWCMXYJ2vNRBN1-PyMO5TDmheH9_AppuXQd9WrX4loB21kVTLLyOyd_WbMecxaXFaEqGJKy7FO078n1Yf6D755FJtQpuzmNv-WdgGMqAB_rpzn9DnkzcQjMF', 1911, 'Bến Nhà Rồng — bến cảng xưa và nay', 1),
('22222222-2222-2222-2222-222222222202', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBO-UNu_GKC60dQ4L1bJxsTQ90dO4MWtIEvZAwYM3Qg171BbOAO5xzCrk6SzxGfQpnA9TRFor3k7Xe-KU8v1RqEl2kW-uteDhKe5lCaSpJ8abetRUKpfJ1CfYG821e_xg6nHyIPler32hZPF58fO1nZKU0YAM1CMSqME3ZF7nzA7jYYJFA8xdc_PV-nvsvvcS9PqP2ptuYh-an4GG1Oi6dONhDfS72A-wmFhhopt-PuSM3BE3NYJJlrVEoPwtOXJMUiIafde493Kzuy', 1844, 'Chùa Thiên Mụ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222203', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuCTOrzxLL7GTvS8QqJbZIcC6-3kdMUNc2qVuGsm6Py8ILXlr4dQA9lBclCQclZ4nR29DUai3kT_EZ0DGPSv5r7TYsAHDO1Gqh7U1ydlAHDdcsabbDsaO8lx4NqQJdybI5MAfxk00ku9guojpAE8Di8n8_LTIo3dXxsS7h16Irxb2xLmSlveMdmvAzdeUKiqxzVT1gj6FQ6ydQpoyUbVlRbqFPcr0Sr2WwZ9wYoYsWftxeYFZLD6mysBhJ4LWDOIl7Nzb3BT65wwOuNQ', 968, 'Cố đô Hoa Lư — xưa và nay', 1),
('22222222-2222-2222-2222-222222222204', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuCtlazSyN49NMSznx4q_qIX0cqxcv62yFHJVbZ30w2DU9rpKRBzc7frzufipWZRmQq5Ov3zFVQB2CwE5crbcFSEMPsOetdhEiemM-YXFa1qhBoourRNMAKIEF7TiPxbU2Q8Rk4J9SDrBGwZC9v9a4_vCBLGGMRNoF3OgGYO2vULBVyKts8t9NXaxmTPDCSUfcw1cAYRXe2qnl7K5nJRHQ6P9nzUpFXOlLwTFvXjRBTjLNQfiWAXKnyNWavouncL-8PVDGEIpgA2lW49', 1010, 'Hoàng Thành Thăng Long — xưa và nay', 1),
('22222222-2222-2222-2222-222222222205', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAqpI-gTLL99nlJth0gz0khOghupmSDWDg5PnGghGIZRtj3zWCluJ4VEf10pvI9-IIoSzs2TqPGwVvhkq28gZ_NH0-o9GOgybv3uEoMS_bqkNdmKVDieQI0XVmr-VbksO6WqB84xzEJTI0ZMYHGStwEikW3FMwm1uOaiqJ-La-WcKTgP-pviKHHPFWXGBjMcm4LvsQpxHzPQw7mXx1GJEAUJtGrjE0Md5CumRsfqpk4vmDXBVYeZkM3T7MUyLC7PeDTW2ebUineVjvB', 1600, 'Phố cổ Hội An — xưa và nay', 1),
('22222222-2222-2222-2222-222222222206', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuBDydkSCyfuTv0QZwuRlZTRSzGdQhQTZwc-NH2mSfkHDV5MVEPaxXcI30FVdAett-CGd9JowEM0X-NuukVppUYjTGQp-xI0XdiZ-S4aZMbCgRJKH8PiKopDUD90uGcIlEGK7uZvspFdLU23zfKBKOgNtKKRMnk7kSBImvp7EOdnahhWYghOeYu-Bmi13XEUY7OprZMEDbEOfP2OOFfjjz_rd3DN9ZqS8lD_sfeZJ2woDaP-9kk7ds1v3cR42ihdcK6CIz4oKVjzx9lo', 1397, 'Thành Nhà Hồ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222207', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAxqMuKQE7yaBYgoARD2p7iFeUE3t_ZLcY6txP8x4mv0uKrSskfSH5ygnZ3INbt63snbS2GSmhdlwbpWacgIQ_8oj0kap8YpsqSE6o73SDS1orZIJVOiWpz-qU7iGV_G9pmnFM_lPaCN0gA2v-_Ec6fNywAnEdJtsw-zYIfgs8jOVWVr2hCLui0KVYJdvXB9qv_PTXiTSl4-GEWwKkrF3AD43ElE_JqaXowOXFNwlA6BEdj2CWhdsGq3O8xLeuBhD14kx2hP9suLd5o', 1070, 'Văn Miếu — xưa và nay', 1),
('22222222-2222-2222-2222-222222222208', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAc8mKVcLTVTDnMzvyQGag3DjLMzJtLi8a8xMlmLzyHao_nUe9-Eaahie-NgxUG3lpPwGksZdwsHz733qOUi107c1JXCnwQWJwlAjPg2bIR3m-_aucswEu33aBeO1v7U_9nEGlA2mmPcOBGb16TyqEqdQniS4Pb8DrvcoG3MoMpFTpkxvVuvPw_buU1rNHdPq4CnFuFX5WZugGfWIXW-l9wd7dUoVwD8QnJRFFhjt4wtuwt0-krlGuqFH_-VCPHyDeqCIItEJA5XhEn', 1802, 'Đại Nội Huế — xưa và nay', 1),
('22222222-2222-2222-2222-222222222209', 'https://lh3.googleusercontent.com/aida-public/AB6AXuAUsk8_gOmPss8ZBj8fB276C8SQUHAaGuYtuAgM6cEMsaXnEayZxu_t2YpQ-1bfv81s8i0e6qZX462KiU9NefpjR0ReJrytgcvQfXck5lg7yGcYlulUkmgWQjv1Dt5nUCrunjaXmRSA8Tb6EFqBQS1RFnvoZy5rLQyyE8LBlGgVBy7orret7JBxz8uCAmgCCKqYWmMS0lHI4XZ53N5jR2MUqHTAZp1hXrpQjQSswyNcn7deI-j9pBByejddJv19XyynM9XA93WvjTKq', 'https://lh3.googleusercontent.com/aida-public/AB6AXuB8IQqPU5BN2MjIDXg5Ch2nDliGa7nxbjtIoZbQai9CKotO_l5f_REDG1IFKo9RgqKck9KD9tK-pzVLXz0Uc0zIO0HtQ1phK8X6I7ewZYrfLVbQFN9MST18bUvlxDY36XEon7LcT0g2McBgHv5QmXcPXtS7zldrHsKhHsfL2tT-MNe8g6bOQFiC4f5Gkn_nSSgsh1UGVMsk63f6vsKaPySPdoT0MVeFraC2f1iLbD2HLM_lwSil7oIDcKRnQecxFTYFiWcBM1FWfLrV', 2879, 'Đền Hùng — xưa và nay', 1);

COMMIT;

-- END seed: 2026-06-27_heritage_p2_checkin_bonus.sql

-- ============================================================================
-- BEGIN seed: 2026-06-30_cu_chi_supplementary_assets.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-06-30_cu_chi_supplementary_assets.sql
-- ============================================================================
-- Cu Chi: ảnh hiện vật / sinh hoạt từ folder "địa đạo củ chi" + gallery
BEGIN;

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:vu-khi';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi-2.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:cua-bom';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi-3.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:min-gat';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/ham-giai-phau.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key IN ('artifact:tram-xa', 'artifact:den-dau');

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/vot-chong.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key IN ('artifact:chong-tre', 'artifact:lao-tre', 'artifact:bay');

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/lo-chau-mai.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'hotspot:vent';

UPDATE artifacts SET image_url = '/media/cu-chi/gallery/an-uong-duoi-dat.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'hotspot:kitchen';

UPDATE artifacts SET image_url = '/media/cu-chi/gallery/khoai-mi-1.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:cuoc-song-duoi-long-dat';

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES
(
  'a0000001-0000-4000-8000-000000000021',
  '11111111-1111-1111-1111-111111111111',
  'Cối xay thóc dưới lòng đất',
  '/media/cu-chi/artifacts/coi-xay-thoc.jpg',
  'Cối xay thóc — chế biến lương thực tại căn cứ ngầm.',
  'Cuộc sống dưới địa đạo thiếu ánh sáng và thực phẩm. Cối xay thóc giúp chế biến lương thực ngay trong hầm, góp phần duy trì sinh hoạt của hàng nghìn người sống và chiến đấu tại Củ Chi.',
  'artifact:coi-xay-thoc',
  'primary',
  21
),
(
  'a0000001-0000-4000-8000-000000000022',
  '11111111-1111-1111-1111-111111111111',
  'Khoai mì — lương thực chiến khu',
  '/media/cu-chi/gallery/khoai-mi-2.jpg',
  'Khoai mì luộc muối mè — món ăn quen thuộc của cư dân địa đạo.',
  'Khoai mì là lương thực chính trong thời chiến tại Củ Chi. Du khách ngày nay vẫn được thử món khoai mì luộc muối mè — hương vị gắn với ký ức thời kháng chiến.',
  'artifact:khoai-mi',
  'primary',
  22
),
(
  'a0000001-0000-4000-8000-000000000023',
  '11111111-1111-1111-1111-111111111111',
  'Ngụy trang hầm chiến đấu',
  '/media/cu-chi/gallery/nguy-trang-ham-1.jpg',
  'Hệ thống ngụy trang tinh vi — khó phát hiện từ trên mặt đất.',
  'Dưới lớp đất và lá che phủ là cả một hệ thống cơ quan phức tạp: hầm chỉ huy, thông hơi, lối thoát hiểm. Ngụy trang giúp địa đạo trở thành "thành phố ngầm" bất khả xâm phạm.',
  'artifact:nguy-trang',
  'primary',
  23
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('artifact:vu-khi', 'artifact:coi-xay-thoc', '11111111-1111-1111-1111-111111111111'),
('hotspot:kitchen', 'artifact:khoai-mi', '11111111-1111-1111-1111-111111111111'),
('hotspot:vent', 'artifact:nguy-trang', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

COMMIT;

-- END seed: 2026-06-30_cu_chi_supplementary_assets.sql

-- ============================================================================
-- BEGIN seed: 2026-07-04_location_unlock.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-04_location_unlock.sql
-- ============================================================================
-- Location progression: unlock after completing prerequisite quest (FR-15)
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS unlock_prerequisite_quest_id UUID REFERENCES quests(id),
    ADD COLUMN IF NOT EXISTS unlock_narrative TEXT;

-- Demo pair: Bến Nhà Rồng locked until Củ Chi quest complete
UPDATE locations
SET unlock_prerequisite_quest_id = '33333333-3333-3333-3333-333333333333',
    unlock_narrative = 'Manh mối từ Địa đạo Củ Chi dẫn bạn đến Bến Nhà Rồng — nơi Bác Hồ ra đi tìm đường cứu nước...'
WHERE id = '22222222-2222-2222-2222-222222222201';

UPDATE locations
SET unlock_prerequisite_quest_id = NULL,
    unlock_narrative = NULL
WHERE id = '11111111-1111-1111-1111-111111111111';

-- END seed: 2026-07-04_location_unlock.sql

-- ============================================================================
-- BEGIN seed: 2026-07-04_discovery_bindings_enrich.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-04_discovery_bindings_enrich.sql
-- ============================================================================
-- Discovery content bindings enrich (BR-09)
ALTER TABLE discovery_content_bindings
    ADD COLUMN IF NOT EXISTS artifact_id UUID NULL,
    ADD COLUMN IF NOT EXISTS xp_bonus INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quest_step_id UUID NULL;

-- Backfill artifact_id from discovery_artifact_links when unlock_key matches
UPDATE discovery_content_bindings b
SET artifact_id = a.id
FROM discovery_artifact_links l
JOIN artifacts a ON a.unlock_key = l.artifact_unlock_key
WHERE l.discovery_unlock_key = b.unlock_key
  AND (b.location_id IS NULL OR b.location_id = l.location_id OR b.location_id = a.location_id)
  AND b.artifact_id IS NULL;

-- Default XP bonus for engagement bindings
UPDATE discovery_content_bindings
SET xp_bonus = 10
WHERE xp_bonus = 0 AND engagement IS NOT NULL;

-- END seed: 2026-07-04_discovery_bindings_enrich.sql

-- ============================================================================
-- BEGIN seed: 2026-07-04_quest_onsite_flag.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-04_quest_onsite_flag.sql
-- ============================================================================
-- Quest onsite requirement flag (BR-12)
ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS require_onsite_checkin BOOLEAN NOT NULL DEFAULT false;

UPDATE quests
SET require_onsite_checkin = true
WHERE LOWER(completion_trigger) = 'checkin';

-- END seed: 2026-07-04_quest_onsite_flag.sql

-- ============================================================================
-- BEGIN seed: 2026-07-04_profile_tier.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-04_profile_tier.sql
-- ============================================================================
-- Freemium tier on profiles (FREE / PREMIUM)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS tier VARCHAR(20) NOT NULL DEFAULT 'FREE';

UPDATE profiles SET tier = 'FREE' WHERE tier IS NULL OR tier = '';

-- END seed: 2026-07-04_profile_tier.sql

-- ============================================================================
-- BEGIN seed: 2026-07-04_org_members_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-04_org_members_seed.sql
-- ============================================================================
-- Seed demo B2B org membership (cu-chi-demo) + teacher account
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, tier, created_at)
VALUES (
    'teacher@histar.vn',
    '$2a$10$pNehDXSRLXQgtSVewDj/guZ1jaOBusRg5fj0UUguwTrZwchjZOlgG',
    'local',
    'TEACHER',
    'Giáo viên Demo',
    NULL,
    1,
    0,
    'TP.HCM',
    'FREE',
    now()
)
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role;

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    p.id,
    'teacher',
    now(),
    now()
FROM profiles p
WHERE p.email = 'teacher@histar.vn'
ON CONFLICT (organization_id, user_id) DO UPDATE SET org_role = EXCLUDED.org_role;

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    p.id,
    'student',
    now(),
    now()
FROM profiles p
WHERE p.role = 'USER'
  AND p.email NOT IN ('teacher@histar.vn')
ORDER BY p.created_at
LIMIT 5
ON CONFLICT (organization_id, user_id) DO NOTHING;

-- END seed: 2026-07-04_org_members_seed.sql

-- ============================================================================
-- BEGIN seed: 2026-07-05_ensure_admin_accounts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-05_ensure_admin_accounts.sql
-- ============================================================================
-- Ensure platform admin accounts have role ADMIN and known demo password.
-- Run after self-registration may have created these emails as USER.

UPDATE profiles
SET role = 'ADMIN',
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    display_name = COALESCE(NULLIF(display_name, ''), 'Platform Admin')
WHERE email = 'admin@histar.vn';

INSERT INTO profiles (email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at)
VALUES (
    'demo@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'ADMIN',
    'FREE',
    'Demo Admin',
    10,
    999,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET
    role = 'ADMIN',
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

UPDATE profiles
SET role = 'ADMIN'
WHERE email IN ('admin@histar.vn', 'demo@histar.vn');

-- This seed can be re-run after Flyway has added email verification columns.
-- Use dynamic SQL so the same file also remains compatible with the pristine
-- bootstrap schema where those columns do not exist until V9.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'profiles'
          AND column_name = 'email_verified'
    ) THEN
        EXECUTE $sql$
            UPDATE profiles
            SET email_verified = TRUE,
                email_verified_at = COALESCE(email_verified_at, now())
            WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn')
        $sql$;
    END IF;
END $$;

-- Demo teacher account (E2E role-ui tests)
UPDATE profiles
SET password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'TEACHER',
    display_name = COALESCE(NULLIF(display_name, ''), 'Giáo viên Demo')
WHERE email = 'teacher@histar.vn';

-- END seed: 2026-07-05_ensure_admin_accounts.sql

-- ============================================================================
-- BEGIN seed: 2026-07-06_org_rbac_and_groups.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-06_org_rbac_and_groups.sql
-- ============================================================================
-- Org RBAC + study groups (Phase C)

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8) UNIQUE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS invite_code_expires_at TIMESTAMPTZ;

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organizations(id);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS org_subscription VARCHAR(16) DEFAULT 'NONE';

CREATE TABLE IF NOT EXISTS study_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(6) UNIQUE NOT NULL,
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS study_group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_study_group_members_user ON study_group_members(user_id);

UPDATE organizations
SET invite_code = 'CUCHI26A',
    invite_code_expires_at = now() + interval '90 days',
    plan = 'org_basic'
WHERE slug = 'cu-chi-demo' AND invite_code IS NULL;

-- END seed: 2026-07-06_org_rbac_and_groups.sql

-- ============================================================================
-- BEGIN seed: 2026-07-07_monetization_p0.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-07_monetization_p0.sql
-- ============================================================================
-- Monetization P0: usage quotas, B2C/B2B subscriptions, org plan limits

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) DEFAULT 'MICRO';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ccu INT DEFAULT 15;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_verified_accounts INT DEFAULT 100;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ai_queries_per_month INT DEFAULT 5000;

UPDATE organizations SET plan_type = 'MICRO' WHERE plan_type IS NULL;
UPDATE organizations SET plan_type = 'STANDARD' WHERE LOWER(COALESCE(plan, '')) IN ('org_pro', 'pro', 'standard');
UPDATE organizations SET plan_type = 'MICRO' WHERE LOWER(COALESCE(plan, '')) IN ('org_basic', 'basic', 'trial', 'micro');
UPDATE organizations SET max_ccu = 40, max_verified_accounts = 400, max_ai_queries_per_month = 30000
  WHERE plan_type = 'STANDARD';
UPDATE organizations SET max_ccu = 80, max_verified_accounts = 1000, max_ai_queries_per_month = NULL
  WHERE plan_type = 'PREMIUM';

CREATE TABLE IF NOT EXISTS b2c_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) UNIQUE,
    price_vnd INT NOT NULL DEFAULT 79000,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    plan_type VARCHAR(20) NOT NULL,
    price_vnd BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS usage_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID REFERENCES profiles(id),
    year INT NOT NULL,
    month INT NOT NULL,
    day INT,
    used_ai_queries INT NOT NULL DEFAULT 0,
    CONSTRAINT usage_quotas_scope_chk CHECK (
        (organization_id IS NOT NULL AND user_id IS NULL)
        OR (organization_id IS NULL AND user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_org_month
    ON usage_quotas (organization_id, year, month)
    WHERE organization_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_user_day
    ON usage_quotas (user_id, year, month, day)
    WHERE user_id IS NOT NULL AND day IS NOT NULL;

-- END seed: 2026-07-07_monetization_p0.sql

-- ============================================================================
-- BEGIN seed: 2026-07-08_quest_steps_schema.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-08_quest_steps_schema.sql
-- ============================================================================
-- Quest + QuestStep entities: normalized step rows per quest (Cu Chi campaigns, etc.)

CREATE TABLE IF NOT EXISTS quest_steps (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quest_id      UUID NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    step_order    INT,
    unlock_key    VARCHAR(255) NOT NULL,
    title         VARCHAR(255),
    objective     VARCHAR(500),
    description   TEXT,
    hint          TEXT,
    action_type   VARCHAR(50),
    action_label  VARCHAR(100),
    xp_partial    INT,
    chat_prompt   TEXT,
    portal_era    INT,
    preview_image TEXT
);

CREATE INDEX IF NOT EXISTS idx_quest_steps_quest_order
    ON quest_steps (quest_id, step_order);

CREATE UNIQUE INDEX IF NOT EXISTS uq_quest_steps_quest_unlock
    ON quest_steps (quest_id, unlock_key);

-- END seed: 2026-07-08_quest_steps_schema.sql

-- ============================================================================
-- BEGIN seed: 2026-07-08_quest_steps_init.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-08_quest_steps_init.sql
-- ============================================================================
BEGIN;

-- Không xoá gì nữa: DELETE FROM quests kéo theo xoá user_quest_progress (ON DELETE CASCADE).
-- Chạy lại chỉ thêm phần còn thiếu. Nội dung chuẩn của 334/335 nằm ở Flyway V84__quest_tracks_v2_content.sql
-- (id bước cố định, bước cổng 1968 là tùy chọn Premium); file này chỉ giữ để bootstrap có 334/335 trước V72.

-- 1. CHIẾN DỊCH 2: MẬT MÃ SA BÀN TÁC CHIẾN (Chrono kể 3 tầng)
INSERT INTO quests (id, location_id, title, description, story, points_reward, required_order, step_discovery_keys, completion_trigger, steps_total, cover_image) VALUES
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111111', 'Mật mã Sa bàn Tác chiến', 'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.', 'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".', 150, 2, 'dialogue:anh-ba,tour:sa-ban,era:1968', 'discovery', 3, '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective, description, hint, action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image) VALUES
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333334', 1, 'dialogue:anh-ba', 'Chương 1 · Hỏi Chrono về các tầng', 'Nghe Chrono kể cấu trúc 3 tầng và độ sâu tầng sâu nhất.', 'Tầng sâu nhất sâu 8–10 m, tối đa 12 m.', 'Gửi câu hỏi đã soạn. Chrono kể theo tư liệu.', 'dialogue', 'Hỏi Chrono', 30, 'Chrono ơi, địa đạo Củ Chi có mấy tầng, tầng sâu nhất sâu bao nhiêu?', NULL, '/media/characters/nam-du-kich.png'),
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333334', 2, 'tour:sa-ban', 'Chương 2 · Khảo sát Bẫy Chông', 'Tìm mảnh giấu gần nhà sa bàn trên sơ đồ, rồi đưa mã cho Chrono.', 'Đứng trong tour không đủ. Mảnh nằm gần nhà biểu diễn sa bàn.', 'Tìm mảnh gần nhà sa bàn, dán mã cho Chrono.', 'tour', 'Tìm mảnh trên sơ đồ', 50, NULL, NULL, '/media/heritage/dia-dao-cu-chi/cover.jpeg'),
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333334', 3, 'era:1968', 'Chương 3 · Phục kích Xe Tăng', 'Mở Time Portal xem cảnh phục kích xe tăng M41.', 'Tái hiện khoảnh khắc quân ta dùng chiến thuật du kích phá hủy xe tăng.', 'Mở Portal chọn mốc thời gian 1968.', 'portal', 'Mở Time Portal', 70, NULL, 1968, '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (quest_id, unlock_key) DO NOTHING;


-- 2. CHIẾN DỊCH 3: ÁNH SÁNG ĐÈN MÙ U (Chrono kể Bếp Hoàng Cầm)
INSERT INTO quests (id, location_id, title, description, story, points_reward, required_order, step_discovery_keys, completion_trigger, steps_total, cover_image) VALUES
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111111', 'Ánh sáng Đèn Mù U', 'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.', 'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.', 120, 3, 'dialogue:chi-nam,tour:tram-xa,era:1968', 'discovery', 3, '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective, description, hint, action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image) VALUES
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333335', 1, 'dialogue:chi-nam', 'Chương 1 · Hỏi Chrono về bếp Hoàng Cầm', 'Nghe Chrono kể cách Bếp Hoàng Cầm giấu khói.', 'Khói đi theo rãnh, lọc qua đất ẩm, thoát lên chỉ còn hơi như sương sớm.', 'Gửi câu hỏi đã soạn cho Chrono.', 'dialogue', 'Hỏi Chrono', 30, 'Chrono ơi, Bếp Hoàng Cầm dẫn khói theo rãnh và lọc qua đất ẩm như thế nào?', NULL, '/media/characters/nu-du-kich.png'),
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333335', 2, 'tour:tram-xa', 'Chương 2 · Trạm xá Dã chiến', 'Tìm kiếm trạm y tế dưới lòng đất trong Tour 360°.', 'Nơi các bác sĩ quân y cứu chữa thương binh trong điều kiện thiếu thốn.', 'Khám phá khu vực Trạm xá trong sa bàn 360°.', 'tour', 'Vào Tour 360°', 40, NULL, NULL, '/media/heritage/dia-dao-cu-chi/cover.jpeg'),
                                                                                                                                                                                       (gen_random_uuid(), '33333333-3333-3333-3333-333333333335', 3, 'era:1968', 'Chương 3 · Lớp học bình dân', 'Mở Time Portal chứng kiến lớp học dưới ánh đèn mù u.', 'Sự kiên cường của những đứa trẻ học chữ dưới làn bom đạn.', 'Mở Portal mốc 1968 tại khu vực Lớp học.', 'portal', 'Mở Time Portal', 50, NULL, 1968, '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

COMMIT;

-- END seed: 2026-07-08_quest_steps_init.sql

-- ============================================================================
-- BEGIN seed: 2026-07-09_shedlock.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-09_shedlock.sql
-- ============================================================================
-- ShedLock table for multi-instance scheduled jobs (Render / prod Postgres).
-- Also applied via Flyway: classpath:db/migration/V11__shedlock.sql

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- END seed: 2026-07-09_shedlock.sql

-- ============================================================================
-- BEGIN seed: 2026-07-10_visit_sessions_ai_fields.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-10_visit_sessions_ai_fields.sql
-- ============================================================================
-- Backfill visit_sessions columns expected by current BE entity/runtime.

ALTER TABLE visit_sessions
    ADD COLUMN IF NOT EXISTS persona_goal VARCHAR(255),
    ADD COLUMN IF NOT EXISTS session_duration VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ai_tone VARCHAR(64);

-- END seed: 2026-07-10_visit_sessions_ai_fields.sql

-- ============================================================================
-- BEGIN seed: 2026-07-10_demo_billing_accounts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-07-10_demo_billing_accounts.sql
-- ============================================================================
-- Demo billing accounts (dev/local/Render seed) — password: Demo@2026
-- 1) premium@histar.vn       — B2C Premium (tier + active subscription, no expiry practical)
-- 2) org-standard@histar.vn  — B2B Standard org owner (ACTIVE org + subscription)

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organizations(id),
    ADD COLUMN IF NOT EXISTS org_subscription VARCHAR(16) DEFAULT 'NONE';

-- ---------------------------------------------------------------------------
-- B2C Premium
-- ---------------------------------------------------------------------------
INSERT INTO profiles (
    id, email, password_hash, provider, role, tier, display_name,
    level, total_points, city, email_verified, email_verified_at, created_at
)
VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'premium@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'USER',
    'PREMIUM',
    'Premium Demo',
    1,
    0,
    'TP.HCM',
    TRUE,
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'USER',
    tier = 'PREMIUM',
    org_id = NULL,
    org_subscription = 'NONE',
    email_verified = TRUE,
    email_verified_at = COALESCE(profiles.email_verified_at, now()),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

UPDATE b2c_subscriptions
SET is_active = FALSE
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- id phải set tường minh: một số DB tạo bảng không có DEFAULT gen_random_uuid()
INSERT INTO b2c_subscriptions (id, user_id, price_vnd, start_date, end_date, is_active, payment_method, created_at)
SELECT
    gen_random_uuid(),
    p.id,
    79000,
    CURRENT_DATE,
    DATE '2099-12-31',
    TRUE,
    'DEMO',
    now()
FROM profiles p
WHERE p.email = 'premium@histar.vn'
  AND NOT EXISTS (
      SELECT 1
      FROM b2c_subscriptions s
      WHERE s.user_id = p.id
        AND s.is_active = TRUE
  );

UPDATE b2c_subscriptions
SET end_date = DATE '2099-12-31',
    is_active = TRUE,
    payment_method = 'DEMO'
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- ---------------------------------------------------------------------------
-- B2B Standard org + owner
-- ---------------------------------------------------------------------------
INSERT INTO organizations (
    id, slug, name, plan, plan_type, contact_email, status,
    max_ccu, max_verified_accounts, max_ai_queries_per_month,
    plan_start_date, plan_end_date, created_at
)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'demo-standard-org',
    'Demo Standard Org',
    'standard',
    'STANDARD',
    'org-standard@histar.vn',
    'ACTIVE',
    40,
    400,
    30000,
    CURRENT_DATE,
    DATE '2099-12-31',
    now()
)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    plan = EXCLUDED.plan,
    plan_type = EXCLUDED.plan_type,
    contact_email = EXCLUDED.contact_email,
    status = EXCLUDED.status,
    max_ccu = EXCLUDED.max_ccu,
    max_verified_accounts = EXCLUDED.max_verified_accounts,
    max_ai_queries_per_month = EXCLUDED.max_ai_queries_per_month,
    plan_start_date = EXCLUDED.plan_start_date,
    plan_end_date = EXCLUDED.plan_end_date;

INSERT INTO profiles (
    id, email, password_hash, provider, role, tier, display_name,
    org_id, org_subscription, level, total_points, city,
    email_verified, email_verified_at, created_at
)
SELECT
    'd2222222-2222-2222-2222-222222222222'::uuid,
    'org-standard@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'TEACHER',
    'FREE',
    'Org Standard Demo',
    o.id,
    'STANDARD',
    1,
    0,
    'TP.HCM',
    TRUE,
    now(),
    now()
FROM organizations o
WHERE o.slug = 'demo-standard-org'
ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'TEACHER',
    tier = 'FREE',
    org_id = EXCLUDED.org_id,
    org_subscription = 'STANDARD',
    email_verified = TRUE,
    email_verified_at = COALESCE(profiles.email_verified_at, now()),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    o.id,
    p.id,
    'teacher',
    now(),
    now()
FROM profiles p
JOIN organizations o ON o.slug = 'demo-standard-org'
WHERE p.email = 'org-standard@histar.vn'
ON CONFLICT (organization_id, user_id) DO UPDATE SET
    org_role = EXCLUDED.org_role,
    accepted_at = COALESCE(organization_members.accepted_at, EXCLUDED.accepted_at);

UPDATE subscriptions
SET is_active = FALSE
WHERE organization_id = (SELECT id FROM organizations WHERE slug = 'demo-standard-org')
  AND is_active = TRUE;

INSERT INTO subscriptions (
    id, organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method, created_at
)
SELECT
    gen_random_uuid(),
    o.id,
    'STANDARD',
    15000000,
    CURRENT_DATE,
    DATE '2099-12-31',
    TRUE,
    'DEMO',
    now()
FROM organizations o
WHERE o.slug = 'demo-standard-org'
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      WHERE s.organization_id = o.id
        AND s.is_active = TRUE
  );

UPDATE subscriptions
SET end_date = DATE '2099-12-31',
    is_active = TRUE,
    payment_method = 'DEMO',
    plan_type = 'STANDARD'
WHERE organization_id = (SELECT id FROM organizations WHERE slug = 'demo-standard-org')
  AND is_active = TRUE;

COMMIT;

-- END seed: 2026-07-10_demo_billing_accounts.sql
BEGIN;

-- ============================================================================
-- BEGIN migration: V1__monetization_history_ready.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V1__monetization_history_ready.sql
-- ============================================================================
-- Production-ready monetization migration baseline.
-- Keeps existing data, enables Flyway on non-empty databases,
-- and converts B2C subscriptions to support real history.

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) DEFAULT 'MICRO';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ccu INT DEFAULT 15;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_verified_accounts INT DEFAULT 100;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ai_queries_per_month INT DEFAULT 5000;

UPDATE organizations SET plan_type = 'MICRO' WHERE plan_type IS NULL;
UPDATE organizations SET plan_type = 'STANDARD' WHERE LOWER(COALESCE(plan, '')) IN ('org_pro', 'pro', 'standard');
UPDATE organizations SET plan_type = 'MICRO' WHERE LOWER(COALESCE(plan, '')) IN ('org_basic', 'basic', 'trial', 'micro');
UPDATE organizations
SET max_ccu = 40, max_verified_accounts = 400, max_ai_queries_per_month = 30000
WHERE plan_type = 'STANDARD'
  AND (max_ccu IS NULL OR max_verified_accounts IS NULL OR max_ai_queries_per_month IS NULL);
UPDATE organizations
SET max_ccu = 80, max_verified_accounts = 1000, max_ai_queries_per_month = NULL
WHERE plan_type = 'PREMIUM'
  AND (max_ccu IS NULL OR max_verified_accounts IS NULL);

CREATE TABLE IF NOT EXISTS b2c_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id),
    price_vnd INT NOT NULL DEFAULT 79000,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

UPDATE b2c_subscriptions
SET created_at = NOW()
WHERE created_at IS NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT tc.constraint_name
    INTO constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu
      ON tc.constraint_name = ccu.constraint_name
     AND tc.table_schema = ccu.table_schema
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'b2c_subscriptions'
      AND tc.constraint_type = 'UNIQUE'
      AND ccu.column_name = 'user_id'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.b2c_subscriptions DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

DROP INDEX IF EXISTS uq_b2c_subscriptions_user_id;
DROP INDEX IF EXISTS b2c_subscriptions_user_id_key;

CREATE INDEX IF NOT EXISTS idx_b2c_subscriptions_user_created_at
    ON b2c_subscriptions (user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_subscriptions_active_user
    ON b2c_subscriptions (user_id)
    WHERE is_active IS TRUE;

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    plan_type VARCHAR(20) NOT NULL,
    price_vnd BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

UPDATE subscriptions
SET created_at = NOW()
WHERE created_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_org_created_at
    ON subscriptions (organization_id, created_at DESC);

CREATE TABLE IF NOT EXISTS usage_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID REFERENCES profiles(id),
    year INT NOT NULL,
    month INT NOT NULL,
    day INT,
    used_ai_queries INT NOT NULL DEFAULT 0,
    CONSTRAINT usage_quotas_scope_chk CHECK (
        (organization_id IS NOT NULL AND user_id IS NULL)
        OR (organization_id IS NULL AND user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_org_month
    ON usage_quotas (organization_id, year, month)
    WHERE organization_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_user_day
    ON usage_quotas (user_id, year, month, day)
    WHERE user_id IS NOT NULL AND day IS NOT NULL;

-- END migration: V1__monetization_history_ready.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V2__add_sepay_b2c_payment_transactions.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V2__add_sepay_b2c_payment_transactions.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS b2c_payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id),
    provider VARCHAR(30) NOT NULL,
    order_code VARCHAR(64) NOT NULL,
    transfer_content VARCHAR(64) NOT NULL,
    amount_vnd INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    return_to_path VARCHAR(255),
    qr_url TEXT,
    provider_transaction_id BIGINT,
    provider_reference_code VARCHAR(255),
    provider_gateway VARCHAR(100),
    provider_payload TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_order_code
    ON b2c_payment_transactions (order_code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_transfer_content
    ON b2c_payment_transactions (transfer_content);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_provider_tx
    ON b2c_payment_transactions (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_b2c_payment_transactions_user_created_at
    ON b2c_payment_transactions (user_id, created_at DESC);

-- END migration: V2__add_sepay_b2c_payment_transactions.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V3__add_billing_settings.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V3__add_billing_settings.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS billing_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('b2c_premium_price_vnd', '79000', NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- END migration: V3__add_billing_settings.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V4__add_org_payment_transactions.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V4__add_org_payment_transactions.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS org_payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_user_id UUID NOT NULL REFERENCES profiles(id),
    organization_id UUID REFERENCES organizations(id),
    org_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    plan_type VARCHAR(20) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    order_code VARCHAR(64) NOT NULL,
    transfer_content VARCHAR(64) NOT NULL,
    amount_vnd BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    return_to_path VARCHAR(255),
    qr_url TEXT,
    provider_transaction_id BIGINT,
    provider_reference_code VARCHAR(255),
    provider_gateway VARCHAR(100),
    provider_payload TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_payment_transactions_order_code
    ON org_payment_transactions (order_code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_payment_transactions_transfer_content
    ON org_payment_transactions (transfer_content);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_payment_transactions_provider_tx
    ON org_payment_transactions (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_org_payment_transactions_user_created_at
    ON org_payment_transactions (requester_user_id, created_at DESC);

-- END migration: V4__add_org_payment_transactions.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V5__add_chat_free_daily_limit_setting.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V5__add_chat_free_daily_limit_setting.sql
-- ============================================================================
INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('chat_free_daily_limit', '10', NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- END migration: V5__add_chat_free_daily_limit_setting.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V6__multiplayer_b2b2c_volume_discount.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V6__multiplayer_b2b2c_volume_discount.sql
-- ============================================================================
-- V6: Multiplayer org teams, B2B2C inquiries, volume discount settings

ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS quest_id UUID;
ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS team_mode VARCHAR(24) DEFAULT 'QUEST';

CREATE INDEX IF NOT EXISTS idx_study_groups_org_id ON study_groups(org_id);
CREATE INDEX IF NOT EXISTS idx_study_groups_quest_id ON study_groups(quest_id);

CREATE TABLE IF NOT EXISTS heritage_digitization_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(64),
    package_type VARCHAR(32) NOT NULL,
    message TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('org_volume_discount_percent', '35', NOW())
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('org_volume_discount_min_licenses', '3', NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- END migration: V6__multiplayer_b2b2c_volume_discount.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V7__ccu_sessions_lms_assignments.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V7__ccu_sessions_lms_assignments.sql
-- ============================================================================
-- V7: Real CCU sessions + LMS Premium assignments

CREATE TABLE IF NOT EXISTS org_active_sessions (
    org_id UUID NOT NULL,
    user_id UUID NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (org_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_org_active_sessions_last_seen ON org_active_sessions(last_seen_at);

CREATE TABLE IF NOT EXISTS assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    due_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_assignments_org_id ON assignments(org_id);
CREATE INDEX IF NOT EXISTS idx_assignments_teacher_id ON assignments(teacher_id);

CREATE TABLE IF NOT EXISTS assignment_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    assignment_id UUID NOT NULL REFERENCES assignments(id) ON DELETE CASCADE,
    student_id UUID NOT NULL,
    score INTEGER,
    auto_graded BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (assignment_id, student_id)
);

CREATE INDEX IF NOT EXISTS idx_assignment_submissions_assignment ON assignment_submissions(assignment_id);
CREATE INDEX IF NOT EXISTS idx_assignment_submissions_student ON assignment_submissions(student_id);

-- END migration: V7__ccu_sessions_lms_assignments.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V8__location_geo_and_maps_url.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V8__location_geo_and_maps_url.sql
-- ============================================================================
-- V8: Địa chỉ + Google Maps URL + tọa độ GPS chuẩn cho 10 di tích

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS formatted_address TEXT,
    ADD COLUMN IF NOT EXISTS google_maps_url TEXT;

-- Địa đạo Củ Chi
UPDATE locations SET
    latitude = 11.141591,
    longitude = 106.4615963,
    formatted_address = 'Khu di tích lịch sử Địa đạo Củ Chi, Phú Mỹ Hưng, Củ Chi, TP. Hồ Chí Minh',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=11.141591,106.4615963'
WHERE id = '11111111-1111-1111-1111-111111111111';

-- Bến Nhà Rồng
UPDATE locations SET
    latitude = 10.766813,
    longitude = 106.706726,
    formatted_address = '1 Nguyễn Tất Thành, phường 12, quận 4, TP. Hồ Chí Minh',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=10.766813,106.706726'
WHERE id = '22222222-2222-2222-2222-222222222201';

-- Chùa Thiên Mụ
UPDATE locations SET
    latitude = 16.453453,
    longitude = 107.558618,
    formatted_address = 'Kim Long, phường Hương Long, TP. Huế, Thừa Thiên Huế',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=16.453453,107.558618'
WHERE id = '22222222-2222-2222-2222-222222222202';

-- Cố Đô Hoa Lư
UPDATE locations SET
    latitude = 20.281923,
    longitude = 105.919373,
    formatted_address = 'xã Trường Yên, huyện Hoa Lư, tỉnh Ninh Bình',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=20.281923,105.919373'
WHERE id = '22222222-2222-2222-2222-222222222203';

-- Hoàng Thành Thăng Long
UPDATE locations SET
    latitude = 21.036578,
    longitude = 105.817288,
    formatted_address = '19C phố Hoàng Diệu, phường Điện Biên, quận Ba Đình, Hà Nội',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.036578,105.817288'
WHERE id = '22222222-2222-2222-2222-222222222204';

-- Phố Cổ Hội An
UPDATE locations SET
    latitude = 15.877157,
    longitude = 108.32934,
    formatted_address = 'phường Minh An, TP. Hội An, tỉnh Quảng Nam',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=15.877157,108.32934'
WHERE id = '22222222-2222-2222-2222-222222222205';

-- Thành Nhà Hồ
UPDATE locations SET
    latitude = 20.079722,
    longitude = 105.604167,
    formatted_address = 'xã Vĩnh Lộc, huyện Vĩnh Lộc, tỉnh Thanh Hóa',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=20.079722,105.604167'
WHERE id = '22222222-2222-2222-2222-222222222206';

-- Văn Miếu Quốc Tử Giám
UPDATE locations SET
    latitude = 21.027954,
    longitude = 105.835754,
    formatted_address = '58 phố Quốc Tử Giám, phường Văn Miếu - Quốc Tử Giám, quận Đống Đa, Hà Nội',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.027954,105.835754'
WHERE id = '22222222-2222-2222-2222-222222222207';

-- Đại Nội Huế
UPDATE locations SET
    latitude = 16.469617,
    longitude = 107.579412,
    formatted_address = 'phố Hùng Vương, phường Thuận Thành, TP. Huế, Thừa Thiên Huế',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=16.469617,107.579412'
WHERE id = '22222222-2222-2222-2222-222222222208';

-- Đền Hùng Vương
UPDATE locations SET
    latitude = 21.353353,
    longitude = 105.314722,
    formatted_address = 'Khu di tích Đền Hùng, xã Hy Cường, huyện Lâm Thao, tỉnh Phú Thọ',
    google_maps_url = 'https://www.google.com/maps/search/?api=1&query=21.353353,105.314722'
WHERE id = '22222222-2222-2222-2222-222222222209';

-- END migration: V8__location_geo_and_maps_url.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V9__email_verification.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V9__email_verification.sql
-- ============================================================================
-- Email verification + optional Firebase UID (Phase 2)

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user
    ON email_verification_tokens(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_email_verification_tokens_hash
    ON email_verification_tokens(token_hash);

-- Seed / admin accounts treated as verified
UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn');

-- END migration: V9__email_verification.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V10__repair_email_verified_column.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V10__repair_email_verified_column.sql
-- ============================================================================
-- Repair partial schema when Hibernate ddl-auto ran before Flyway V9 completed.
-- Safe to run on DBs where V9 already applied (IF NOT EXISTS / idempotent updates).

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);

UPDATE profiles SET email_verified = FALSE WHERE email_verified IS NULL;

UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn');

ALTER TABLE profiles ALTER COLUMN email_verified SET DEFAULT FALSE;
ALTER TABLE profiles ALTER COLUMN email_verified SET NOT NULL;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user
    ON email_verification_tokens(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_email_verification_tokens_hash
    ON email_verification_tokens(token_hash);

-- END migration: V10__repair_email_verified_column.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V11__shedlock.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V11__shedlock.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);

-- END migration: V11__shedlock.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V12__add_b2c_renewal_reminders.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V12__add_b2c_renewal_reminders.sql
-- ============================================================================
-- Add B2C renewal reminder persistence
-- Needed because Hibernate ddl-auto=validate requires table existence.

CREATE TABLE IF NOT EXISTS b2c_renewal_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES b2c_subscriptions(id),
    reminder_day INT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_renewal_reminders_subscription_day
    ON b2c_renewal_reminders (subscription_id, reminder_day);

-- END migration: V12__add_b2c_renewal_reminders.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V13__visit_sessions_ai_fields.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V13__visit_sessions_ai_fields.sql
-- ============================================================================
-- Align visit_sessions with current VisitSession entity fields.

ALTER TABLE visit_sessions
    ADD COLUMN IF NOT EXISTS persona_goal VARCHAR(255),
    ADD COLUMN IF NOT EXISTS session_duration VARCHAR(32),
    ADD COLUMN IF NOT EXISTS ai_tone VARCHAR(64);

-- END migration: V13__visit_sessions_ai_fields.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V14__quest_steps.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V14__quest_steps.sql
-- ============================================================================
-- QuestStep entity — see docs/database/2026-07-08_quest_steps_schema.sql

CREATE TABLE IF NOT EXISTS quest_steps (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quest_id      UUID NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    step_order    INT,
    unlock_key    VARCHAR(255) NOT NULL,
    title         VARCHAR(255),
    objective     VARCHAR(500),
    description   TEXT,
    hint          TEXT,
    action_type   VARCHAR(50),
    action_label  VARCHAR(100),
    xp_partial    INT,
    chat_prompt   TEXT,
    portal_era    INT,
    preview_image TEXT
);

CREATE INDEX IF NOT EXISTS idx_quest_steps_quest_order
    ON quest_steps (quest_id, step_order);

CREATE UNIQUE INDEX IF NOT EXISTS uq_quest_steps_quest_unlock
    ON quest_steps (quest_id, unlock_key);

-- END migration: V14__quest_steps.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V15__tour360_multi_scene.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V15__tour360_multi_scene.sql
-- ============================================================================
SET client_encoding TO 'UTF8';

-- Tour 360: panorama metadata + hotspot marker_style + corrected route + Đền sub-scenes
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS area_slug VARCHAR(64);
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS sort_order INT DEFAULT 0;
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_yaw DOUBLE PRECISION DEFAULT 0;
ALTER TABLE panoramas ADD COLUMN IF NOT EXISTS default_pitch DOUBLE PRECISION DEFAULT 0;

ALTER TABLE hotspots ADD COLUMN IF NOT EXISTS marker_style VARCHAR(10);

-- Macro + Đền sub-scenes (giữ UUID macro 221-227; thêm 228-22c cho Đền t2-t6)
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bai-xe/bai-xe-so-1.jpg', 'Bãi gửi xe gắn máy số 1', 'bai-xe', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t1-cong-vao.jpg', 'Cổng vào Đền Bến Dược', 'den-ben-duoc', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t2-bia.jpg', 'Trước bia tưởng niệm', 'den-ben-duoc', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t3.jpg', 'Trước đền (ghép panorama)', 'den-ben-duoc', 3, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222a', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t4.jpg', 'Trước đền', 'den-ben-duoc', 4, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222b', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t5.jpg', 'Bắc đền', 'den-ben-duoc', 5, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222c', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/den-ben-duoc/den-t6.jpg', 'Trong đền', 'den-ben-duoc', 6, 0, -0.05) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-trung-bay/kb-nga-ba.jpg', 'Ngã ba Khu trưng bày', 'khu-trung-bay', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222d', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-trung-bay/kb-trong-khu.jpg', 'Khu trưng bày', 'khu-trung-bay', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bo-tu-lenh/bo-tu-lenh.jpg', 'Căn cứ Bộ Tư lệnh QK SG-GĐ', 'bo-tu-lenh', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-22222222222e', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/bo-tu-lenh/btl-trung-bay-2.jpg', 'Trưng bày bom & đạn', 'bo-tu-lenh', 2, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-uy/khu-uy.jpg', 'Căn cứ Khu ủy QK SG-GĐ', 'khu-uy', 1, 0, -0.18) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/nha-bieu-dien/nha-bieu-dien.jpg', 'Nhà biểu diễn Sa bàn, Phim 3D', 'nha-bieu-dien', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;
INSERT INTO panoramas (id, location_id, image_url, title, area_slug, sort_order, default_yaw, default_pitch) VALUES ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111111', '/media/cu-chi/panoramas/khu-tai-hien/khu-tai-hien.jpg', 'Khu tái hiện Vùng Giải phóng', 'khu-tai-hien', 1, 0, 0) ON CONFLICT (id) DO UPDATE SET image_url = EXCLUDED.image_url, title = EXCLUDED.title, area_slug = EXCLUDED.area_slug, sort_order = EXCLUDED.sort_order, default_yaw = EXCLUDED.default_yaw, default_pitch = EXCLUDED.default_pitch;

-- Clear old scene links for tour panoramas
DELETE FROM hotspots WHERE type = 'scene' AND panorama_id IN ('22222222-2222-2222-2222-222222222221', '22222222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222228', '22222222-2222-2222-2222-222222222229', '22222222-2222-2222-2222-22222222222a', '22222222-2222-2222-2222-22222222222b', '22222222-2222-2222-2222-22222222222c', '22222222-2222-2222-2222-222222222224', '22222222-2222-2222-2222-22222222222d', '22222222-2222-2222-2222-222222222223', '22222222-2222-2222-2222-22222222222e', '22222222-2222-2222-2222-222222222225', '22222222-2222-2222-2222-222222222226', '22222222-2222-2222-2222-222222222227');

-- Links (forward + backward)
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label, marker_style) VALUES
('22222222-2222-2222-2222-222222222221', 0.42, -0.55, 'scene', '22222222-2222-2222-2222-222222222222', '→ Đền Bến Dược', 'far'),
('22222222-2222-2222-2222-222222222222', 2.69, -0.55, 'scene', '22222222-2222-2222-2222-222222222221', '→ Bãi xe', 'far'),
('22222222-2222-2222-2222-222222222222', -2.69, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '→ Khu trưng bày', 'far'),
('22222222-2222-2222-2222-222222222222', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222228', '→ Trong đền', 'near'),
('22222222-2222-2222-2222-222222222228', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222229', '→ Trước đền', 'near'),
('22222222-2222-2222-2222-222222222229', 3.14159, -0.42, 'scene', '22222222-2222-2222-2222-22222222222a', '→ Trước đền', 'near'),
('22222222-2222-2222-2222-222222222229', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-222222222228', '← Quay lại', 'near'),
('22222222-2222-2222-2222-22222222222a', 0.0, -0.42, 'scene', '22222222-2222-2222-2222-22222222222b', '→ Bắc đền', 'near'),
('22222222-2222-2222-2222-22222222222a', 3.14159, -0.42, 'scene', '22222222-2222-2222-2222-222222222229', '← Quay lại', 'near'),
('22222222-2222-2222-2222-22222222222b', 0.12, -0.42, 'scene', '22222222-2222-2222-2222-22222222222c', '→ Trong đền', 'near'),
('22222222-2222-2222-2222-222222222224', 1.174, -0.55, 'scene', '22222222-2222-2222-2222-222222222222', '← Quay lại Đền', 'far'),
('22222222-2222-2222-2222-222222222224', 1.674, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '→ Bộ Tư lệnh', 'far'),
('22222222-2222-2222-2222-222222222224', -0.963, -0.55, 'scene', '22222222-2222-2222-2222-22222222222d', '→ Tham quan khu trưng bày', 'far'),
('22222222-2222-2222-2222-22222222222d', 1.389, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '← Quay lại ngã ba', 'far'),
('22222222-2222-2222-2222-222222222223', 0.55, -0.55, 'scene', '22222222-2222-2222-2222-222222222225', '→ Khu ủy', 'far'),
('22222222-2222-2222-2222-222222222223', 0.85, -0.55, 'scene', '22222222-2222-2222-2222-22222222222e', '→ Trưng bày bom & đạn', 'far'),
('22222222-2222-2222-2222-22222222222e', -0.85, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '← Quay lại', 'far'),
('22222222-2222-2222-2222-222222222225', -0.35, -0.55, 'scene', '22222222-2222-2222-2222-222222222226', '→ Nhà biểu diễn', 'far'),
('22222222-2222-2222-2222-222222222226', 0.22, -0.55, 'scene', '22222222-2222-2222-2222-222222222227', '→ Khu tái hiện', 'far'),
('22222222-2222-2222-2222-222222222228', 3.141592653589793, -0.42, 'scene', '22222222-2222-2222-2222-222222222222', '← Quay lại', 'near'),
('22222222-2222-2222-2222-22222222222b', 3.141592653589793, -0.42, 'scene', '22222222-2222-2222-2222-22222222222a', '← Quay lại', 'near'),
('22222222-2222-2222-2222-22222222222c', 3.2615926535897932, -0.42, 'scene', '22222222-2222-2222-2222-22222222222b', '← Quay lại', 'near'),
('22222222-2222-2222-2222-222222222223', 4.8156, -0.55, 'scene', '22222222-2222-2222-2222-222222222224', '← Quay lại', 'far'),
('22222222-2222-2222-2222-222222222225', 3.691592653589793, -0.55, 'scene', '22222222-2222-2222-2222-222222222223', '← Quay lại', 'far'),
('22222222-2222-2222-2222-222222222226', 2.791592653589793, -0.55, 'scene', '22222222-2222-2222-2222-222222222225', '← Quay lại', 'far'),
('22222222-2222-2222-2222-222222222227', 3.3615926535897933, -0.55, 'scene', '22222222-2222-2222-2222-222222222226', '← Quay lại', 'far');

-- END migration: V15__tour360_multi_scene.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V16__b2c_premium_price_79000.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V16__b2c_premium_price_79000.sql
-- ============================================================================
-- Raise default B2C Premium price from 49.000đ to 79.000đ/tháng.
-- Updates runtime billing_settings used by public pricing + SePay QR amount.

UPDATE billing_settings
SET setting_value = '79000',
    updated_at = NOW()
WHERE setting_key = 'b2c_premium_price_vnd'
  AND setting_value IN ('49000', '49_000');

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
SELECT 'b2c_premium_price_vnd', '79000', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM billing_settings WHERE setting_key = 'b2c_premium_price_vnd'
);

-- END migration: V16__b2c_premium_price_79000.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V17__cu_chi_scene_discovery_points.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V17__cu_chi_scene_discovery_points.sql
-- ============================================================================
-- Award discovery XP for every Củ Chi 360 scene, once per user.
-- V15 added 14 panoramas; only scene:…221 / …222 / …223 already exist where the
-- gamification seed was applied. CREATE is a no-op when that seed already ran.
CREATE TABLE IF NOT EXISTS discovery_points (
    id          UUID PRIMARY KEY,
    location_id UUID NOT NULL,
    name        VARCHAR(255) NOT NULL,
    map_x_pct   NUMERIC(5,2) NOT NULL DEFAULT 50,
    map_y_pct   NUMERIC(5,2) NOT NULL DEFAULT 50,
    unlock_key  VARCHAR(128) NOT NULL,
    sort_order  INT NOT NULL DEFAULT 0,
    UNIQUE (location_id, unlock_key)
);

INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000013', '11111111-1111-1111-1111-111111111111', 'Ngã ba Khu trưng bày', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222224', 13),
('d0000001-0000-4000-8000-000000000014', '11111111-1111-1111-1111-111111111111', 'Căn cứ Khu ủy', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222225', 14),
('d0000001-0000-4000-8000-000000000015', '11111111-1111-1111-1111-111111111111', 'Nhà biểu diễn', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222226', 15),
('d0000001-0000-4000-8000-000000000016', '11111111-1111-1111-1111-111111111111', 'Khu tái hiện', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222227', 16),
('d0000001-0000-4000-8000-000000000017', '11111111-1111-1111-1111-111111111111', 'Trước bia tưởng niệm', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222228', 17),
('d0000001-0000-4000-8000-000000000018', '11111111-1111-1111-1111-111111111111', 'Trước đền (ghép panorama)', 50.00, 50.00, 'scene:22222222-2222-2222-2222-222222222229', 18),
('d0000001-0000-4000-8000-000000000019', '11111111-1111-1111-1111-111111111111', 'Trước đền', 50.00, 50.00, 'scene:22222222-2222-2222-2222-22222222222a', 19),
('d0000001-0000-4000-8000-00000000001a', '11111111-1111-1111-1111-111111111111', 'Bắc đền', 50.00, 50.00, 'scene:22222222-2222-2222-2222-22222222222b', 20),
('d0000001-0000-4000-8000-00000000001b', '11111111-1111-1111-1111-111111111111', 'Trong đền', 50.00, 50.00, 'scene:22222222-2222-2222-2222-22222222222c', 21),
('d0000001-0000-4000-8000-00000000001c', '11111111-1111-1111-1111-111111111111', 'Khu trưng bày', 50.00, 50.00, 'scene:22222222-2222-2222-2222-22222222222d', 22),
('d0000001-0000-4000-8000-00000000001d', '11111111-1111-1111-1111-111111111111', 'Trưng bày bom và đạn', 50.00, 50.00, 'scene:22222222-2222-2222-2222-22222222222e', 23)
ON CONFLICT (location_id, unlock_key) DO NOTHING;

-- END migration: V17__cu_chi_scene_discovery_points.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V18__cu_chi_hotspot_yaw_pitch_patch.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V18__cu_chi_hotspot_yaw_pitch_patch.sql
-- ============================================================================
-- Patch 5 scene-link hotspots that commonly point the wrong way (Epic A calibrate).
-- Values from field calibrate pass (?calibrate=1) + spread overlapping BTL markers.

-- Bãi xe → Đền: nudge toward temple approach
UPDATE hotspots
SET yaw = 0.55, pitch = -0.48, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222222';

-- Đền cổng → Khu trưng bày: leftward far marker
UPDATE hotspots
SET yaw = -2.45, pitch = -0.50, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222224';

-- Ngã ba trưng bày → Bộ Tư lệnh: separate from overlapping siblings
UPDATE hotspots
SET yaw = 1.95, pitch = -0.48, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222223';

-- Bộ Tư lệnh → Khu ủy
UPDATE hotspots
SET yaw = 0.35, pitch = -0.45, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222225';

-- Bộ Tư lệnh → Trưng bày bom: spread from Khu ủy marker
UPDATE hotspots
SET yaw = 1.15, pitch = -0.45, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-22222222222e';

-- END migration: V18__cu_chi_hotspot_yaw_pitch_patch.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V19__b2b2c_inquiry_admin_fields.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V19__b2b2c_inquiry_admin_fields.sql
-- ============================================================================
ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS admin_notes TEXT;

ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS contacted_at TIMESTAMPTZ NULL;

-- END migration: V19__b2b2c_inquiry_admin_fields.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V20__b2c_premium_49000_and_payment_subscription.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V20__b2c_premium_49000_and_payment_subscription.sql
-- ============================================================================
-- Gate 0 SePay entitlement hardening.
-- 1) B2C Premium price is 49.000đ/tháng (supersedes V16's 79.000đ).
-- 2) Link a paid SePay transaction to the b2c_subscriptions row it activated.
--    Status UNDERPAID is a code-level value on b2c_payment_transactions.status (VARCHAR), no schema change.

UPDATE billing_settings
SET setting_value = '49000',
    updated_at = NOW()
WHERE setting_key = 'b2c_premium_price_vnd';

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
SELECT 'b2c_premium_price_vnd', '49000', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM billing_settings WHERE setting_key = 'b2c_premium_price_vnd'
);

ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS subscription_id UUID NULL REFERENCES b2c_subscriptions(id);

CREATE INDEX IF NOT EXISTS idx_b2c_payment_transactions_subscription_id
    ON b2c_payment_transactions (subscription_id)
    WHERE subscription_id IS NOT NULL;

-- END migration: V20__b2c_premium_49000_and_payment_subscription.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V21__stations_content_blocks.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V21__stations_content_blocks.sql
-- ============================================================================
-- A3: Củ Chi pilot stations + content blocks.
CREATE TABLE IF NOT EXISTS stations (
    id             UUID PRIMARY KEY,
    site_code      VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    code           VARCHAR(32)  NOT NULL,
    name           VARCHAR(255) NOT NULL,
    sort_order     INT          NOT NULL DEFAULT 0,
    lat            DOUBLE PRECISION,
    lng            DOUBLE PRECISION,
    quest_step_key VARCHAR(128),
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_stations_site_code UNIQUE (site_code, code)
);

CREATE TABLE IF NOT EXISTS station_content_blocks (
    id         UUID PRIMARY KEY,
    station_id UUID         NOT NULL REFERENCES stations (id) ON DELETE CASCADE,
    block_type VARCHAR(16)  NOT NULL,
    title      VARCHAR(255),
    body       TEXT,
    media_url  VARCHAR(1024),
    sort_order INT          NOT NULL DEFAULT 0,
    meta_json  TEXT,
    CONSTRAINT chk_station_block_type CHECK (block_type IN ('TEXT', 'IMAGE', 'AUDIO', 'VIDEO', 'HOTSPOT'))
);

CREATE INDEX IF NOT EXISTS idx_station_content_blocks_station
    ON station_content_blocks (station_id, sort_order);

INSERT INTO stations (id, site_code, code, name, sort_order, quest_step_key, is_active) VALUES
('a3000001-0000-4000-8000-000000000001', 'cu-chi', 'ST01', 'Bến Dược', 1, 'cu-chi:ST01', TRUE),
('a3000001-0000-4000-8000-000000000002', 'cu-chi', 'ST02', 'Địa đạo', 2, 'cu-chi:ST02', TRUE),
('a3000001-0000-4000-8000-000000000003', 'cu-chi', 'ST03', 'Bếp Hoàng Cầm', 3, 'cu-chi:ST03', TRUE),
('a3000001-0000-4000-8000-000000000004', 'cu-chi', 'ST04', 'Giếng nước', 4, 'cu-chi:ST04', TRUE),
('a3000001-0000-4000-8000-000000000005', 'cu-chi', 'ST05', 'Phòng họp', 5, 'cu-chi:ST05', TRUE),
('a3000001-0000-4000-8000-000000000006', 'cu-chi', 'ST06', 'Xuất phát', 6, 'cu-chi:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3000002-0000-4000-8000-000000000001', 'a3000001-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu Bến Dược',
 'Nội dung giới thiệu tạm thời cho trạm Bến Dược. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000002', 'a3000001-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu Địa đạo',
 'Nội dung giới thiệu tạm thời cho trạm Địa đạo. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000003', 'a3000001-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu Bếp Hoàng Cầm',
 'Nội dung giới thiệu tạm thời cho trạm Bếp Hoàng Cầm. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000004', 'a3000001-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu Giếng nước',
 'Nội dung giới thiệu tạm thời cho trạm Giếng nước. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000005', 'a3000001-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu Phòng họp',
 'Nội dung giới thiệu tạm thời cho trạm Phòng họp. Sẽ được cập nhật bởi đội nội dung.', 1),
('a3000002-0000-4000-8000-000000000006', 'a3000001-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu Xuất phát',
 'Nội dung giới thiệu tạm thời cho trạm Xuất phát. Sẽ được cập nhật bởi đội nội dung.', 1)
ON CONFLICT (id) DO NOTHING;

-- END migration: V21__stations_content_blocks.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V22__checkin_presence.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V22__checkin_presence.sql
-- ============================================================================
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

-- END migration: V22__checkin_presence.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V23__pilot_events.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V23__pilot_events.sql
-- ============================================================================
CREATE TABLE pilot_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_uuid UUID UNIQUE,
    user_id UUID,
    session_id UUID,
    event_type VARCHAR(64) NOT NULL,
    station_code VARCHAR(32),
    payload_json TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_pilot_events_event_type ON pilot_events (event_type);
CREATE INDEX idx_pilot_events_occurred_at ON pilot_events (occurred_at);
CREATE INDEX idx_pilot_events_user_id ON pilot_events (user_id);

-- END migration: V23__pilot_events.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V24__story_chapters.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V24__story_chapters.sql
-- ============================================================================
-- B4: Story chapters mapped to stations; chapters 1-2 free, 3+ require Premium.
CREATE TABLE IF NOT EXISTS story_chapters (
    id               UUID PRIMARY KEY,
    site_code        VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    chapter_number   INT          NOT NULL,
    station_code     VARCHAR(32)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    synopsis         TEXT,
    requires_premium BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order       INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_story_chapters_site_number UNIQUE (site_code, chapter_number),
    CONSTRAINT fk_story_chapters_station FOREIGN KEY (site_code, station_code)
        REFERENCES stations (site_code, code)
);

CREATE INDEX IF NOT EXISTS idx_story_chapters_site_sort
    ON story_chapters (site_code, sort_order);

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a4000001-0000-4000-8000-000000000001', 'cu-chi', 1, 'ST01',
 'Một ngày dưới lòng đất – Chương 1: Bến Dược thức giấc',
 'Bình minh trên Bến Dược, nơi cuộc hành trình một ngày dưới lòng đất bắt đầu. Bạn nhận nhiệm vụ đầu tiên từ người liên lạc và bước vào vùng đất thép.',
 FALSE, 1),
('a4000001-0000-4000-8000-000000000002', 'cu-chi', 2, 'ST02',
 'Một ngày dưới lòng đất – Chương 2: Bước xuống địa đạo',
 'Cửa hầm hé mở. Bạn cúi người chui xuống mạng lưới địa đạo chằng chịt, làm quen với bóng tối, hơi đất và nhịp thở của những người đã sống ở đây.',
 FALSE, 2),
('a4000001-0000-4000-8000-000000000003', 'cu-chi', 3, 'ST03',
 'Một ngày dưới lòng đất – Chương 3: Bếp Hoàng Cầm không khói',
 'Bữa cơm giữa chiến trường: bí mật của chiếc bếp không khói và những đêm nấu ăn lặng lẽ để không lộ vị trí.',
 TRUE, 3),
('a4000001-0000-4000-8000-000000000004', 'cu-chi', 4, 'ST04',
 'Một ngày dưới lòng đất – Chương 4: Giọt nước giữa chiến hào',
 'Giếng nước – nguồn sống của cả khu căn cứ. Bạn theo chân người tải nước trong đêm và hiểu vì sao mỗi giọt nước đều quý.',
 TRUE, 4),
('a4000001-0000-4000-8000-000000000005', 'cu-chi', 5, 'ST05',
 'Một ngày dưới lòng đất – Chương 5: Ánh đèn phòng họp',
 'Dưới ánh đèn dầu leo lét, các chỉ huy vạch kế hoạch cho trận đánh. Bạn được chứng kiến một cuộc họp quyết định.',
 TRUE, 5),
('a4000001-0000-4000-8000-000000000006', 'cu-chi', 6, 'ST06',
 'Một ngày dưới lòng đất – Chương 6: Xuất phát',
 'Kết thúc một ngày dưới lòng đất, đội hình xuất phát trong đêm. Bạn trở lại mặt đất mang theo câu chuyện của Củ Chi.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;

-- END migration: V24__story_chapters.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V25__pgvector_rag.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V25__pgvector_rag.sql
-- ============================================================================
-- B5: pgvector RAG (verified sources -> chunks -> cosine search), station chat chips, offline FAQ, quality log, semantic cache.
--
-- HOST NOTE: CREATE EXTENSION vector needs the pgvector package installed on the Postgres host
-- (Supabase/Neon/Railway pgvector images: available; Render managed Postgres: enable it in the dashboard or
-- use a pgvector-capable instance). If this statement fails on a host without pgvector, install/enable the
-- extension first, then re-run Flyway. The app keeps working with rag.enabled=false (default) as long as the
-- tables exist; embeddings are nullable and search always filters `embedding IS NOT NULL`.
CREATE EXTENSION IF NOT EXISTS vector;

-- ---------------------------------------------------------------------------
-- Verified sources (only rows with verified_at NOT NULL are retrievable by the RAG service)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS rag_sources (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title        VARCHAR(255) NOT NULL,
    license      VARCHAR(128),
    verified_by  VARCHAR(128),
    verified_at  TIMESTAMPTZ,
    era          VARCHAR(64),
    station_code VARCHAR(32),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rag_sources_station ON rag_sources (station_code);

CREATE TABLE IF NOT EXISTS rag_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    source_id    UUID NOT NULL REFERENCES rag_sources (id) ON DELETE CASCADE,
    station_code VARCHAR(32),
    era          VARCHAR(64),
    content      TEXT NOT NULL,
    embedding    vector(768),
    chunk_index  INT  NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_rag_chunks_source  ON rag_chunks (source_id);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_station ON rag_chunks (station_code);
-- Pilot corpus is tiny (hundreds of chunks): exact scan is fast and exact. Add an HNSW index when it grows:
--   CREATE INDEX idx_rag_chunks_embedding ON rag_chunks USING hnsw (embedding vector_cosine_ops);

-- ---------------------------------------------------------------------------
-- Suggested question chips per station / persona
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS station_chat_prompts (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code     VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    station_code  VARCHAR(32)  NOT NULL,
    persona       VARCHAR(64)  NOT NULL DEFAULT 'chi-nam',
    chip_label    VARCHAR(80)  NOT NULL,
    question_text VARCHAR(500) NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_station_chat_prompts UNIQUE (site_code, station_code, persona, sort_order)
);

CREATE INDEX IF NOT EXISTS idx_station_chat_prompts_lookup
    ON station_chat_prompts (site_code, station_code, persona, sort_order);

-- ---------------------------------------------------------------------------
-- Quality log (no raw question text: only a hash) and semantic cache
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS chat_quality_log (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    question_hash VARCHAR(64) NOT NULL,
    station_code  VARCHAR(32),
    chunks_used   INT         NOT NULL DEFAULT 0,
    had_citation  BOOLEAN     NOT NULL DEFAULT FALSE,
    latency_ms    INT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_chat_quality_log_created ON chat_quality_log (created_at);

CREATE TABLE IF NOT EXISTS semantic_cache (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    embedding    vector(768),
    station_code VARCHAR(32),
    persona      VARCHAR(64) NOT NULL,
    reply_text   TEXT        NOT NULL,
    expires_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_expires ON semantic_cache (expires_at);

-- ---------------------------------------------------------------------------
-- Offline FAQ (shipped in the full offline pack as faq_offline.json)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS faq_offline (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code    VARCHAR(64) NOT NULL DEFAULT 'cu-chi',
    station_code VARCHAR(32),
    question     VARCHAR(500) NOT NULL,
    answer       TEXT         NOT NULL,
    source_refs  TEXT
);

CREATE INDEX IF NOT EXISTS idx_faq_offline_site_station ON faq_offline (site_code, station_code);

-- ---------------------------------------------------------------------------
-- Seed: 3 chips per station ST01-ST06 for persona chi-nam (Cu Chi themed)
-- ---------------------------------------------------------------------------
INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(md5('chip:cu-chi:ST01:chi-nam:1')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Bến Dược là gì?',            'Chị Năm ơi, Bến Dược là nơi như thế nào và vì sao nổi tiếng?', 1),
(md5('chip:cu-chi:ST01:chi-nam:2')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Nhận nhiệm vụ đầu tiên',     'Khi mới đến Bến Dược, người chiến sĩ thường nhận nhiệm vụ gì?', 2),
(md5('chip:cu-chi:ST01:chi-nam:3')::uuid, 'cu-chi', 'ST01', 'chi-nam', 'Cuộc sống nơi đây',         'Cuộc sống hằng ngày của bà con và du kích ở vùng đất thép này ra sao?', 3),

(md5('chip:cu-chi:ST02:chi-nam:1')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Địa đạo có mấy tầng?',      'Địa đạo Củ Chi có mấy tầng và sâu bao nhiêu hả Chị Năm?', 1),
(md5('chip:cu-chi:ST02:chi-nam:2')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Vì sao lối xuống nhỏ thế?', 'Vì sao miệng hầm và lối đi trong địa đạo lại nhỏ và hẹp như vậy?', 2),
(md5('chip:cu-chi:ST02:chi-nam:3')::uuid, 'cu-chi', 'ST02', 'chi-nam', 'Sống dưới lòng đất',      'Người ta sinh hoạt, ăn ở và liên lạc thế nào khi sống dưới địa đạo?', 3),

(md5('chip:cu-chi:ST03:chi-nam:1')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Bếp Hoàng Cầm là gì?',      'Bếp Hoàng Cầm là loại bếp gì và vì sao lại đặc biệt?', 1),
(md5('chip:cu-chi:ST03:chi-nam:2')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Nấu ăn không bị lộ khói',   'Làm sao để nấu ăn dưới địa đạo mà khói không bị phát hiện?', 2),
(md5('chip:cu-chi:ST03:chi-nam:3')::uuid, 'cu-chi', 'ST03', 'chi-nam', 'Bữa cơm của du kích',       'Bữa cơm của các chiến sĩ thời đó thường có những món gì?', 3),

(md5('chip:cu-chi:ST04:chi-nam:1')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Giếng nước để làm gì?',     'Giếng nước ở đây có vai trò gì đối với người sống trong địa đạo?', 1),
(md5('chip:cu-chi:ST04:chi-nam:2')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Lấy nước có nguy hiểm không?', 'Việc lấy nước ở đây có nguy hiểm gì và phải giữ bí mật ra sao?', 2),
(md5('chip:cu-chi:ST04:chi-nam:3')::uuid, 'cu-chi', 'ST04', 'chi-nam', 'Nước quý đến mức nào?',    'Vì sao mỗi giọt nước lại quý đến vậy trong những năm chiến tranh?', 3),

(md5('chip:cu-chi:ST05:chi-nam:1')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Phòng họp dùng làm gì?',    'Phòng họp dưới địa đạo dùng để làm gì hả Chị Năm?', 1),
(md5('chip:cu-chi:ST05:chi-nam:2')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Họp dưới ánh đèn dầu',      'Các cuộc họp chỉ huy diễn ra thế nào trong điều kiện thiếu sáng và chật hẹp?', 2),
(md5('chip:cu-chi:ST05:chi-nam:3')::uuid, 'cu-chi', 'ST05', 'chi-nam', 'Quyết định quan trọng',     'Những quyết định quan trọng nào từng được bàn bạc ở những nơi như thế này?', 3),

(md5('chip:cu-chi:ST06:chi-nam:1')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Tóm tắt hành trình',        'Chị Năm tóm tắt giúp em hành trình một ngày dưới lòng đất nhé!', 1),
(md5('chip:cu-chi:ST06:chi-nam:2')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Ý nghĩa của Củ Chi',        'Địa đạo Củ Chi có ý nghĩa gì với lịch sử dân tộc?', 2),
(md5('chip:cu-chi:ST06:chi-nam:3')::uuid, 'cu-chi', 'ST06', 'chi-nam', 'Nên đi tiếp đâu?',          'Sau chuyến tham quan này em nên tìm hiểu thêm điều gì về Củ Chi?', 3)
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: offline FAQ. PLACEHOLDER content (tone + structure); every answer must be re-verified against approved
-- sources (see docs/RAG_PILOT_GATE.md) before the public pilot.
-- ---------------------------------------------------------------------------
INSERT INTO faq_offline (id, site_code, station_code, question, answer, source_refs) VALUES
(md5('faq:cu-chi:global:1')::uuid, 'cu-chi', NULL, 'Địa đạo Củ Chi là gì?',
 'Địa đạo Củ Chi là hệ thống đường hầm ngầm ở vùng Củ Chi (TP. Hồ Chí Minh), được đào và mở rộng trong các cuộc kháng chiến để trú ẩn, liên lạc, sinh hoạt và chiến đấu.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:2')::uuid, 'cu-chi', NULL, 'Hệ thống địa đạo dài bao nhiêu?',
 'Các tài liệu thường nêu tổng chiều dài khoảng 250 km. Con số có thể khác nhau tùy nguồn và thời điểm thống kê nên cần đối chiếu với tài liệu chính thức của Ban quản lý di tích.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:3')::uuid, 'cu-chi', NULL, 'Địa đạo có mấy tầng?',
 'Nhiều đoạn địa đạo được đào thành nhiều tầng ở các độ sâu khác nhau (thường được mô tả khoảng 3 m, 6 m và 8–10 m) để tránh bom đạn và tăng khả năng ẩn náu.',
 'placeholder:cu-chi-overview (chờ xác minh)'),
(md5('faq:cu-chi:global:4')::uuid, 'cu-chi', NULL, 'Nên chuẩn bị gì khi vào địa đạo?',
 'Mang giày thoải mái, nước uống, đèn pin hoặc điện thoại đã sạc pin. Lối đi hẹp, thấp, nóng và ẩm; nếu sợ không gian kín bạn có thể chọn đoạn ngắn hoặc đi ra ở các lối thoát.',
 'placeholder:visitor-tips (chờ xác minh)'),
(md5('faq:cu-chi:global:5')::uuid, 'cu-chi', NULL, 'Mất sóng trong địa đạo thì dùng ứng dụng thế nào?',
 'Hãy vào mục "Chuẩn bị hành trang" và tải gói Lite/Full khi còn Wi-Fi. Ảnh, bản đồ và các câu hỏi thường gặp này sẽ dùng được offline; chat AI cần có kết nối.',
 'placeholder:app-offline-guide'),

(md5('faq:cu-chi:ST01:1')::uuid, 'cu-chi', 'ST01', 'Bến Dược ở đâu?',
 'Khu di tích Địa đạo Bến Dược thuộc huyện Củ Chi, TP. Hồ Chí Minh, là một trong hai khu di tích địa đạo được đón khách tham quan (cùng với Bến Đình).',
 'placeholder:ben-duoc (chờ xác minh)'),
(md5('faq:cu-chi:ST01:2')::uuid, 'cu-chi', 'ST01', 'Ở Bến Dược có thể xem những gì?',
 'Du khách có thể xem các đoạn địa đạo, hầm sinh hoạt, hầm chông, khu tái hiện đời sống và chiến đấu của quân dân Củ Chi.',
 'placeholder:ben-duoc (chờ xác minh)'),

(md5('faq:cu-chi:ST02:1')::uuid, 'cu-chi', 'ST02', 'Vì sao miệng hầm lại nhỏ như vậy?',
 'Miệng hầm được làm nhỏ và ngụy trang kỹ để khó bị phát hiện từ trên mặt đất; kích thước nhỏ cũng giúp người trong hầm dễ phòng thủ.',
 'placeholder:dia-dao (chờ xác minh)'),
(md5('faq:cu-chi:ST02:2')::uuid, 'cu-chi', 'ST02', 'Bên trong địa đạo có những khu vực nào?',
 'Bên cạnh đường hầm chính còn có các khu nghỉ ngơi, hội họp, bếp, hầm cứu thương và giếng nước, được nối với nhau bằng các nhánh hầm.',
 'placeholder:dia-dao (chờ xác minh)'),

(md5('faq:cu-chi:ST03:1')::uuid, 'cu-chi', 'ST03', 'Bếp Hoàng Cầm là gì?',
 'Bếp Hoàng Cầm là kiểu bếp nấu ăn hạn chế khói: khói được dẫn qua hệ thống rãnh dài rồi thoát ra ngoài, giúp đơn vị hạn chế bị phát hiện. Bếp mang tên người sáng tạo ra nó.',
 'placeholder:bep-hoang-cam (chờ xác minh)'),
(md5('faq:cu-chi:ST03:2')::uuid, 'cu-chi', 'ST03', 'Vì sao phải nấu ăn kín đáo như vậy?',
 'Khói bếp là dấu hiệu dễ bị máy bay và lực lượng đối phương phát hiện, nên việc giảm khói và chọn thời điểm nấu phù hợp là yếu tố sống còn.',
 'placeholder:bep-hoang-cam (chờ xác minh)'),

(md5('faq:cu-chi:ST04:1')::uuid, 'cu-chi', 'ST04', 'Giếng nước có vai trò gì?',
 'Giếng là nguồn nước sinh hoạt cho người sống và chiến đấu trong khu vực địa đạo, vì vậy vị trí giếng thường được giữ bí mật và bảo vệ cẩn thận.',
 'placeholder:gieng-nuoc (chờ xác minh)'),
(md5('faq:cu-chi:ST04:2')::uuid, 'cu-chi', 'ST04', 'Vì sao nước lại quý ở đây?',
 'Vùng đất cao, khô vào mùa nắng nên việc có nước sạch khó khăn; lấy nước lại dễ bị lộ vị trí nên mỗi lần lấy nước đều phải tính toán thận trọng.',
 'placeholder:gieng-nuoc (chờ xác minh)'),

(md5('faq:cu-chi:ST05:1')::uuid, 'cu-chi', 'ST05', 'Phòng họp dưới địa đạo dùng làm gì?',
 'Đây là nơi cán bộ, chỉ huy bàn kế hoạch tác chiến, phân công nhiệm vụ và thông báo tình hình cho các đơn vị.',
 'placeholder:phong-hop (chờ xác minh)'),
(md5('faq:cu-chi:ST05:2')::uuid, 'cu-chi', 'ST05', 'Ánh sáng và không khí dưới hầm thế nào?',
 'Hầm sử dụng đèn dầu và các lỗ thông hơi được ngụy trang để lấy không khí; điều kiện vẫn rất chật chội, thiếu sáng và ngột ngạt.',
 'placeholder:phong-hop (chờ xác minh)'),

(md5('faq:cu-chi:ST06:1')::uuid, 'cu-chi', 'ST06', 'Toàn bộ hành trình kéo dài bao lâu?',
 'Tùy nhịp tham quan, cả hành trình sáu trạm thường mất khoảng 1–2 giờ. Đây là ước tính cho trải nghiệm trong app, không phải số liệu chính thức của khu di tích.',
 'placeholder:app-tour-estimate')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- Seed: 2 placeholder sources + chunks WITHOUT embeddings (embedding NULL).
-- verified_at is NULL on purpose: the search query only returns verified sources, so placeholders are never served.
-- scripts/rag-ingest-approved.ps1 replaces them (same title) with approved markdown + embeddings.
-- ---------------------------------------------------------------------------
INSERT INTO rag_sources (id, title, license, verified_by, verified_at, era, station_code) VALUES
('a5000001-0000-4000-8000-000000000001', 'Tổng quan Địa đạo Củ Chi (mẫu)', 'internal-draft', NULL, NULL, '1946-1975', NULL),
('a5000001-0000-4000-8000-000000000002', 'Bếp Hoàng Cầm (mẫu)',            'internal-draft', NULL, NULL, '1946-1975', 'ST03')
ON CONFLICT (id) DO NOTHING;

INSERT INTO rag_chunks (id, source_id, station_code, era, content, chunk_index) VALUES
(md5('chunk:a5000001-1:0')::uuid, 'a5000001-0000-4000-8000-000000000001', NULL, '1946-1975',
 'Địa đạo Củ Chi là hệ thống đường hầm ngầm ở vùng Củ Chi, được đào và mở rộng trong các cuộc kháng chiến để trú ẩn, liên lạc và chiến đấu.', 0),
(md5('chunk:a5000001-1:1')::uuid, 'a5000001-0000-4000-8000-000000000001', NULL, '1946-1975',
 'Nhiều đoạn địa đạo có cấu trúc nhiều tầng, kèm theo các khu sinh hoạt, hội họp, bếp, hầm cứu thương và giếng nước.', 1),
(md5('chunk:a5000001-2:0')::uuid, 'a5000001-0000-4000-8000-000000000002', 'ST03', '1946-1975',
 'Bếp Hoàng Cầm là kiểu bếp nấu ăn hạn chế khói, khói được dẫn qua rãnh dài để tản ra nhằm giảm nguy cơ bị phát hiện.', 0),
(md5('chunk:a5000001-2:1')::uuid, 'a5000001-0000-4000-8000-000000000002', 'ST03', '1946-1975',
 'Việc nấu ăn thường được tính toán về thời điểm và vị trí để hạn chế dấu hiệu khói bếp lộ ra mặt đất.', 1)
ON CONFLICT (id) DO NOTHING;

-- END migration: V25__pgvector_rag.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V26__squads.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V26__squads.sql
-- ============================================================================
-- B7 Tiểu đội co-op (squads)

CREATE TABLE squads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(6) NOT NULL,
    site_code VARCHAR(64),
    leader_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_squads_code UNIQUE (code)
);

CREATE INDEX idx_squads_leader_user_id ON squads (leader_user_id);
CREATE INDEX idx_squads_site_code ON squads (site_code);

CREATE TABLE squad_members (
    squad_id UUID NOT NULL REFERENCES squads (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (squad_id, user_id)
);

CREATE INDEX idx_squad_members_user_id ON squad_members (user_id);

-- END migration: V26__squads.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V27__station_minigames.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V27__station_minigames.sql
-- ============================================================================
-- B6: Station mini-games (one per ST01–ST06) and per-user scores.
CREATE TABLE IF NOT EXISTS station_minigames (
    id            UUID PRIMARY KEY,
    site_code     VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    station_code  VARCHAR(32)  NOT NULL,
    game_type     VARCHAR(32)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    config_json   TEXT         NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_station_minigames_site_station UNIQUE (site_code, station_code),
    CONSTRAINT fk_station_minigames_station FOREIGN KEY (site_code, station_code)
        REFERENCES stations (site_code, code)
);

CREATE INDEX IF NOT EXISTS idx_station_minigames_site_station
    ON station_minigames (site_code, station_code);

CREATE TABLE IF NOT EXISTS station_minigame_scores (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    minigame_id   UUID         NOT NULL REFERENCES station_minigames (id) ON DELETE CASCADE,
    score         INT          NOT NULL,
    completed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_station_minigame_scores_user_game UNIQUE (user_id, minigame_id)
);

CREATE INDEX IF NOT EXISTS idx_station_minigame_scores_user
    ON station_minigame_scores (user_id);

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b6000001-0000-4000-8000-000000000001', 'cu-chi', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm nhanh: Bến Dược',
 '{"timeLimitSec":90,"questions":[{"prompt":"Bến Dược nổi tiếng vì điều gì?","options":["Là cửa ngõ vào vùng đất thép","Là bãi biển du lịch","Là nhà máy xi măng"],"correctIndex":0},{"prompt":"Nhiệm vụ đầu tiên của du kích thường là gì?","options":["Nhận chỉ thị và chuẩn bị hành trang","Mua vé tham quan","Chụp ảnh selfie"],"correctIndex":0},{"prompt":"Cuộc sống vùng Củ Chi thời kháng chiến được mô tả thế nào?","options":["Gắn bó, kiên cường","Chỉ có du lịch","Hoàn toàn yên bình như thành phố"],"correctIndex":0}]}',
 1),
('b6000001-0000-4000-8000-000000000002', 'cu-chi', 'ST02', 'MEMORY_PAIRS',
 'Ghép cặp: Địa đạo Củ Chi',
 '{"pairs":[{"id":"a","label":"Miệng hầm"},{"id":"b","label":"Hầm đi bộ"},{"id":"c","label":"Hầm ngầm"},{"id":"d","label":"Bẫy địa đạo"},{"id":"e","label":"Lối thoát hiểm"},{"id":"f","label":"Phòng y tế"}]}',
 2),
('b6000001-0000-4000-8000-000000000003', 'cu-chi', 'ST03', 'TIMELINE_ORDER',
 'Sắp thứ tự: Bếp Hoàng Cầm',
 '{"items":["Chuẩn bị củi và nồi","Đào hầm thoát khói","Nấu cơm trong đêm","Che kín miệng hầm khói"],"hint":"Theo trình tự nấu ăn bí mật dưới địa đạo."}',
 3),
('b6000001-0000-4000-8000-000000000004', 'cu-chi', 'ST04', 'SPOT_DIFF',
 'Tìm khác biệt: Giếng nước',
 '{"leftTitle":"Ban ngày (an toàn)","rightTitle":"Ban đêm (bí mật)","leftItems":["Giếng có nắp che","Có người canh gác rõ ràng","Xô nước để cạnh giếng","Tiếng kéo nước vang"],"rightItems":["Giếng có nắp che","Không có người canh — lấy nước lén","Xô nước để cạnh giếng","Tiếng kéo nước được hạn chế"],"diffIndices":[1,3]}',
 4),
('b6000001-0000-4000-8000-000000000005', 'cu-chi', 'ST05', 'MATCH_TERMS',
 'Nối thuật ngữ: Phòng họp',
 '{"terms":["Phòng họp dưới đất","Ánh đèn dầu","Kế hoạch tấn công","Liên lạc viên"],"definitions":["Nơi chỉ huy bàn kế hoạch trong địa đạo","Ánh sáng hạn chế, tiết kiệm nhiên liệu","Được thống nhất trước khi xuất kích","Người truyền tin giữa các tổ"],"answerKey":[0,1,2,3]}',
 5),
('b6000001-0000-4000-8000-000000000006', 'cu-chi', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: Xuất phát đêm',
 '{"scenario":"Đội hình sắp rời căn cứ dưới lòng đất. Bạn cần chọn hướng di chuyển an toàn về khu vực Bến Dược.","choices":[{"id":"n","label":"Bắc — theo hầm chính đã được trinh sát","correct":true},{"id":"e","label":"Đông — qua khu đang có tuần tra địch","correct":false},{"id":"s","label":"Nam — lối tắt chưa được kiểm tra","correct":false},{"id":"w","label":"Tây — hướng mở ra vùng trống dễ lộ","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;

-- END migration: V27__station_minigames.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V28__stations_content_refresh.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V28__stations_content_refresh.sql
-- ============================================================================
-- Week 0: align Củ Chi pilot station names with MVBP doc (ST01–ST06 codes unchanged).
UPDATE stations SET name = 'Phòng họp', sort_order = 1
WHERE site_code = 'cu-chi' AND code = 'ST01';

UPDATE stations SET name = 'Kho ngầm', sort_order = 2
WHERE site_code = 'cu-chi' AND code = 'ST02';

UPDATE stations SET name = 'Giếng hầm & thông gió', sort_order = 3
WHERE site_code = 'cu-chi' AND code = 'ST03';

UPDATE stations SET name = 'Quân y', sort_order = 4
WHERE site_code = 'cu-chi' AND code = 'ST04';

UPDATE stations SET name = 'Xưởng vũ khí', sort_order = 5
WHERE site_code = 'cu-chi' AND code = 'ST05';

UPDATE stations SET name = 'Hầm phòng thủ', sort_order = 6
WHERE site_code = 'cu-chi' AND code = 'ST06';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Phòng họp',
    body = 'Trạm mở đầu hành trình: không gian chỉ huy và họp bí mật dưới lòng đất. Nội dung chi tiết sẽ được BQL xác thực sau khảo sát Tuần 0.'
WHERE id = 'a3000002-0000-4000-8000-000000000001';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Kho ngầm',
    body = 'Kho lương thực và khu bếp (nếu nằm trên tuyến tham quan). Chủ đề: nuôi quân trong im lặng, giấu khói và bảo quản lương khô.'
WHERE id = 'a3000002-0000-4000-8000-000000000002';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Giếng hầm & thông gió',
    body = 'Giếng nước, lỗ thông hơi và cách ngụy trang miệng giếng. Chủ đề: nước sạch và không khí cho sinh hoạt dưới hầm.'
WHERE id = 'a3000002-0000-4000-8000-000000000003';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Quân y',
    body = 'Khu chăm sóc người bị thương với điều kiện y tế thiếu thốn. Chủ đề: thuốc thảo dược, sơ cứu dưới ánh đèn dầu.'
WHERE id = 'a3000002-0000-4000-8000-000000000004';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Xưởng vũ khí',
    body = 'Xưởng tái chế vật liệu thành vũ khí và công cụ. Chủ đề: an toàn lao động, quy mô xưởng ngầm.'
WHERE id = 'a3000002-0000-4000-8000-000000000005';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Hầm phòng thủ',
    body = 'Hệ thống hầm phòng thủ, cửa thoát và lỗ quan sát. Chủ đề: trực gác và sinh hoạt khi bị vây.'
WHERE id = 'a3000002-0000-4000-8000-000000000006';

-- END migration: V28__stations_content_refresh.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V29__media_transcode_jobs.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V29__media_transcode_jobs.sql
-- ============================================================================
-- C2b: WebM (MediaRecorder) -> MP4 transcode jobs (FAILED_NO_FFMPEG when ffmpeg is unavailable).
CREATE TABLE IF NOT EXISTS media_transcode_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID REFERENCES profiles (id) ON DELETE SET NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    input_format      VARCHAR(16)  NOT NULL DEFAULT 'webm',
    output_format     VARCHAR(16)  NOT NULL DEFAULT 'mp4',
    input_url         TEXT,
    output_url        TEXT,
    input_size_bytes  BIGINT,
    error_message     TEXT,
    attempts          INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_media_transcode_jobs_status_created
    ON media_transcode_jobs (status, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_media_transcode_jobs_user
    ON media_transcode_jobs (user_id, created_at DESC);

-- END migration: V29__media_transcode_jobs.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V30__device_push_tokens.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V30__device_push_tokens.sql
-- ============================================================================
-- E2: FCM device tokens (one row per user + platform)

CREATE TABLE IF NOT EXISTS device_push_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL,
    platform   VARCHAR(16)  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_push_tokens_user_platform UNIQUE (user_id, platform),
    CONSTRAINT chk_device_push_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX IF NOT EXISTS idx_device_push_tokens_user ON device_push_tokens (user_id);

-- END migration: V30__device_push_tokens.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V31__referral_codes.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V31__referral_codes.sql
-- ============================================================================
-- C4: Creator referral codes + landing visit tracking.
CREATE TABLE IF NOT EXISTS referral_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(64)  NOT NULL UNIQUE,
    creator_name  VARCHAR(255) NOT NULL,
    headline      VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS referral_visits (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referral_code  VARCHAR(64)  NOT NULL REFERENCES referral_codes (code) ON DELETE CASCADE,
    user_id        UUID,
    session_id     VARCHAR(64),
    visited_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_referral_visits_code ON referral_visits (referral_code, visited_at DESC);

INSERT INTO referral_codes (id, code, creator_name, headline) VALUES
('c4000001-0000-4000-8000-000000000001', 'demo', 'HistAR Demo Creator', 'Khám phá địa đạo Củ Chi cùng HistAR')
ON CONFLICT (code) DO NOTHING;

-- END migration: V31__referral_codes.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V32__align_story_minigames_mvbp.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V32__align_story_minigames_mvbp.sql
-- ============================================================================
-- V32: Align story_chapters + station_minigames with MVBP doc §5 / V28 station names.
-- ST01 Phòng họp → ST02 Kho ngầm → ST03 Giếng hầm → ST04 Quân y → ST05 Xưởng vũ khí → ST06 Hầm phòng thủ

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 1: Rạng sáng ở Phòng họp',
    synopsis = 'Ánh đèn dầu leo lét. Bạn chứng kiến cuộc họp bí mật dưới lòng đất: kế hoạch của ngày, mật hiệu và nhiệm vụ liên lạc. Cliffhanger: khẩu hiệu mở kho ngầm là gì?'
WHERE site_code = 'cu-chi' AND chapter_number = 1 AND station_code = 'ST01';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 2: Nuôi quân trong im lặng',
    synopsis = 'Kho ngầm và bếp Hoàng Cầm (nếu trên tuyến): lương khô, giấu khói, nuôi quân không lộ vị trí. Cliffhanger: ống dẫn khói dẫn đi đâu?'
WHERE site_code = 'cu-chi' AND chapter_number = 2 AND station_code = 'ST02';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 3: Nước và hơi thở',
    synopsis = 'Giếng hầm và lỗ thông hơi — nguồn sống dưới địa đạo. Bạn học cách lấy nước và ngụy trang miệng lỗ. Cliffhanger: đâu là lỗ thông hơi thật?'
WHERE site_code = 'cu-chi' AND chapter_number = 3 AND station_code = 'ST03';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 4: Sơ cứu dưới ánh đèn dầu',
    synopsis = 'Quân y: chăm sóc người bị thương với thảo dược và dụng cụ thiếu thốn. Cliffhanger: thứ tự sơ cứu đúng theo tư liệu là gì?'
WHERE site_code = 'cu-chi' AND chapter_number = 4 AND station_code = 'ST04';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 5: Tái chế từ đồng nát',
    synopsis = 'Xưởng vũ khí ngầm: biến vật liệu phế thành công cụ, nhấn mạnh sáng tạo và an toàn lao động. Cliffhanger: mảnh nào ghép thành dụng cụ?'
WHERE site_code = 'cu-chi' AND chapter_number = 5 AND station_code = 'ST05';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 6: Lối thoát an toàn',
    synopsis = 'Hầm phòng thủ nhiều tầng: trực gác, lỗ quan sát và cửa thoát khi bị vây. Kết thúc một ngày dưới lòng đất. Cliffhanger: tuyến nào an toàn nhất?'
WHERE site_code = 'cu-chi' AND chapter_number = 6 AND station_code = 'ST06';

-- Mini-games: titles + config aligned to V28 station themes (doc §4 B6)
UPDATE station_minigames SET
    title = 'Giải mã mật hiệu — Phòng họp',
    game_type = 'MATCH_TERMS',
    config_json = '{"terms":["Phòng họp dưới đất","Ánh đèn dầu","Kế hoạch ngày","Liên lạc viên"],"definitions":["Nơi chỉ huy bàn kế hoạch trong địa đạo","Ánh sáng hạn chế, tiết kiệm nhiên liệu","Được thống nhất trước khi xuất kích","Người truyền tin giữa các tổ"],"answerKey":[0,1,2,3]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST01';

UPDATE station_minigames SET
    title = 'Khói đi đâu? — Kho ngầm & bếp',
    game_type = 'TIMELINE_ORDER',
    config_json = '{"items":["Chuẩn bị củi và nồi","Đào / kiểm ống dẫn khói","Nấu trong đêm im lặng","Che kín miệng hầm khói"],"hint":"Theo trình tự nấu ăn bí mật dưới địa đạo (bếp Hoàng Cầm)."}'
WHERE site_code = 'cu-chi' AND station_code = 'ST02';

UPDATE station_minigames SET
    title = 'Tìm lỗ thông hơi — Giếng hầm',
    game_type = 'SPOT_DIFF',
    config_json = '{"leftTitle":"Bề mặt ngụy trang","rightTitle":"Chi tiết thật","leftItems":["Bụi cây che miệng lỗ","Lỗ thông hơi lộ rõ","Nắp giếng kín","Cỏ khô rải quanh"],"rightItems":["Bụi cây che miệng lỗ","Lỗ thông hơi được ngụy trang","Nắp giếng kín","Cỏ khô rải quanh"],"diffIndices":[1]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST03';

UPDATE station_minigames SET
    title = 'Sơ cứu dưới ánh đèn dầu — Quân y',
    game_type = 'TIMELINE_ORDER',
    config_json = '{"items":["Kiểm tra đường thở","Cầm máu bằng băng gạc","Đắp thảo dược đã duyệt","Chuyển người bị thương an toàn"],"hint":"Thứ tự sơ cứu theo tư liệu quân y địa đạo (đã duyệt)."}'
WHERE site_code = 'cu-chi' AND station_code = 'ST04';

UPDATE station_minigames SET
    title = 'Tái chế từ đồng nát — Xưởng vũ khí',
    game_type = 'MEMORY_PAIRS',
    config_json = '{"pairs":[{"id":"a","label":"Phôi kim loại"},{"id":"b","label":"Khuôn"},{"id":"c","label":"Búa thợ"},{"id":"d","label":"Dây buộc"},{"id":"e","label":"Đèn dầu xưởng"},{"id":"f","label":"Hộp dụng cụ"}]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST05';

UPDATE station_minigames SET
    title = 'Lối thoát an toàn — Hầm phòng thủ',
    game_type = 'COMPASS_CHOICE',
    config_json = '{"scenario":"Hầm bị vây. Bạn cần chọn tuyến thoát an toàn theo sơ đồ nhiều tầng.","choices":[{"id":"n","label":"Hầm chính đã trinh sát","correct":true},{"id":"e","label":"Lối đông đang có tuần tra","correct":false},{"id":"s","label":"Lối tắt chưa kiểm tra","correct":false},{"id":"w","label":"Mở ra vùng trống dễ lộ","correct":false}]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST06';

-- END migration: V32__align_story_minigames_mvbp.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V33__journey_pass_and_site_code.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V33__journey_pass_and_site_code.sql
-- ============================================================================
-- V33: Journey Pass (72h visit entitlement) + locations.site_code for multi-site pilot registry.

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

UPDATE locations SET site_code = 'cu-chi'
WHERE id = '11111111-1111-1111-1111-111111111111' AND (site_code IS NULL OR site_code = '');

UPDATE locations SET site_code = 'hoang-thanh-thang-long'
WHERE id = '22222222-2222-2222-2222-222222222204' AND (site_code IS NULL OR site_code = '');

UPDATE locations SET site_code = 'dai-noi-hue'
WHERE id = '22222222-2222-2222-2222-222222222208' AND (site_code IS NULL OR site_code = '');

CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_site_code
    ON locations (site_code)
    WHERE site_code IS NOT NULL AND site_code <> '';

-- B2C Journey Pass price (default 29.000đ)
INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('b2c_journey_pass_price_vnd', '29000', NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- Visit entitlements: unlock story ch.3–6 + chat citations for one site within a window
CREATE TABLE IF NOT EXISTS b2c_visit_entitlements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    site_code    VARCHAR(64)  NOT NULL,
    source       VARCHAR(32)  NOT NULL DEFAULT 'SEPAY',
    order_code   VARCHAR(64),
    starts_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_b2c_visit_entitlements_site CHECK (site_code ~ '^[a-z0-9][a-z0-9-]{0,63}$')
);

CREATE INDEX IF NOT EXISTS idx_b2c_visit_entitlements_user_site
    ON b2c_visit_entitlements (user_id, site_code, expires_at DESC);

-- Optional plan marker on B2C payment intents (PREMIUM vs JOURNEY_PASS)
ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(32) NOT NULL DEFAULT 'PREMIUM';

ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

-- END migration: V33__journey_pass_and_site_code.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V34__pilot_httl_hue_stations.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V34__pilot_httl_hue_stations.sql
-- ============================================================================
-- V34: Pilot sites — Hoàng thành Thăng Long + Đại Nội Huế (same format as Cu Chi ST01–ST06).
-- Station names follow the approved 3-region draft; story/minigame copy based on public heritage facts
-- (UNESCO / BQL materials). Replace via Admin CMS after BQL legal sign-off if needed.

-- ========== Hoàng thành Thăng Long ==========
INSERT INTO stations (id, site_code, code, name, sort_order, lat, lng, quest_step_key, is_active) VALUES
('a3400001-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 'ST01', 'Cổng tiếp nhận Hoàng Diệu', 1, 21.036578, 105.817288, 'httl:ST01', TRUE),
('a3400001-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 'ST02', 'Khu khảo cổ 18 Hoàng Diệu', 2, 21.037100, 105.834800, 'httl:ST02', TRUE),
('a3400001-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 'ST03', 'Nền Điện Kính Thiên', 3, 21.038200, 105.835200, 'httl:ST03', TRUE),
('a3400001-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 'ST04', 'Bảo tàng Hoàng thành', 4, 21.037800, 105.834500, 'httl:ST04', TRUE),
('a3400001-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 'ST05', 'Hầm chỉ huy', 5, 21.036900, 105.833900, 'httl:ST05', TRUE),
('a3400001-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 'ST06', 'Kỳ đài — Cột cờ Hà Nội', 6, 21.039000, 105.834000, 'httl:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3400002-0000-4000-8000-000000000001', 'a3400001-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu',
 'Hoàng thành Thăng Long là Di sản Thế giới UNESCO — trung tâm chính trị suốt hơn một thiên niên kỷ. Trạm mở đầu giúp bạn định hướng hành trình 6 điểm.', 1),
('a3400002-0000-4000-8000-000000000002', 'a3400001-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu',
 'Khu khảo cổ 18 Hoàng Diệu lộ các tầng văn hóa Lý — Trần — Lê. Móng cột và dấu vết kiến trúc cho thấy sự liên tục của kinh đô.', 1),
('a3400002-0000-4000-8000-000000000003', 'a3400001-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu',
 'Điện Kính Thiên từng là hạt nhân hoàng cung. Ngày nay còn nền đá và đôi rồng đá thời Lê Sơ — biểu tượng quyền lực và thẩm mỹ cung đình.', 1),
('a3400002-0000-4000-8000-000000000004', 'a3400001-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu',
 'Trưng bày ngói men, gốm sứ ngự dụng và hiện vật khai quật — cầu nối giữa khảo cổ học và câu chuyện cung đình.', 1),
('a3400002-0000-4000-8000-000000000005', 'a3400001-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu',
 'Hầm chỉ huy trong khu Hoàng thành gắn với lịch sử kháng chiến hiện đại — lớp ký ức chồng lên di sản cổ.', 1),
('a3400002-0000-4000-8000-000000000006', 'a3400001-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu',
 'Kỳ đài (Cột cờ Hà Nội) xây 1812 thời Gia Long, cao gần 33m — công trình hiếm còn nguyên qua thuộc địa và chiến tranh.', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a3400003-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 1, 'ST01',
 'Một ngày ở Hoàng thành — Chương 1: Cổng Hoàng Diệu',
 'Bạn bước vào khu di sản UNESCO, nhận nhiệm vụ khám phá sáu trạm và hiểu vì sao Thăng Long là trái tim chính trị của Đại Việt.',
 FALSE, 1),
('a3400003-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 2, 'ST02',
 'Một ngày ở Hoàng thành — Chương 2: Tầng đất 18 Hoàng Diệu',
 'Xuống khu khảo cổ: từng lớp đất kể về Lý, Trần, Lê. Bạn học đọc dấu vết móng cột như đọc một cuốn sử dưới chân.',
 FALSE, 2),
('a3400003-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 3, 'ST03',
 'Một ngày ở Hoàng thành — Chương 3: Đôi rồng đá Kính Thiên',
 'Trước nền Điện Kính Thiên, đôi rồng đá thế kỷ XV kể về nghi thức triều đình và thẩm mỹ thời Lê.',
 TRUE, 3),
('a3400003-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 4, 'ST04',
 'Một ngày ở Hoàng thành — Chương 4: Ngói men và bát ngự',
 'Trong bảo tàng, ngói chim phượng và gốm sứ ngự dụng hé lộ đời sống cung đình và kỹ nghệ lò quan.',
 TRUE, 4),
('a3400003-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 5, 'ST05',
 'Một ngày ở Hoàng thành — Chương 5: Lớp ký ức dưới hầm',
 'Hầm chỉ huy chồng lên di sản cổ — bạn đối chiếu hai thời kỳ trong cùng một không gian.',
 TRUE, 5),
('a3400003-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 6, 'ST06',
 'Một ngày ở Hoàng thành — Chương 6: Ngọn cờ trên Kỳ đài',
 'Kết thúc hành trình tại Cột cờ Hà Nội — biểu tượng sống sót qua biến thiên lịch sử.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b3400001-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm: Hoàng thành Thăng Long',
 '{"timeLimitSec":90,"questions":[{"prompt":"Hoàng thành Thăng Long được UNESCO công nhận là gì?","options":["Di sản Thế giới","Khu vui chơi","Bảo tàng tư nhân"],"correctIndex":0},{"prompt":"Địa chỉ chính thức gần với đường nào?","options":["Hoàng Diệu, Ba Đình","Nguyễn Huệ, Q.1","Lê Lợi, Huế"],"correctIndex":0},{"prompt":"Thăng Long từng là gì trong lịch sử?","options":["Kinh đô nhiều triều đại","Chỉ một ngôi chùa","Cảng biển"],"correctIndex":0}]}',
 1),
('b3400001-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 'ST02', 'TIMELINE_ORDER',
 'Sắp thứ tự: tầng văn hóa',
 '{"items":["Lớp văn hóa thời Lý","Lớp văn hóa thời Trần","Lớp văn hóa thời Lê","Lớp khảo cổ hiện đại"],"hint":"Từ sớm đến muộn theo dòng lịch sử kinh đô."}',
 2),
('b3400001-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 'ST03', 'MATCH_TERMS',
 'Nối thuật ngữ: Điện Kính Thiên',
 '{"terms":["Điện Kính Thiên","Đôi rồng đá","Nền điện","Lê Sơ"],"definitions":["Hạt nhân trung tâm hoàng cung xưa","Kiệt tác điêu khắc thế kỷ XV còn lại","Phần kiến trúc còn thấy trên mặt đất","Triều đại gắn với đôi rồng đá"],"answerKey":[0,1,2,3]}',
 3),
('b3400001-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 'ST04', 'MEMORY_PAIRS',
 'Ghép cặp: hiện vật bảo tàng',
 '{"pairs":[{"id":"a","label":"Ngói men xanh"},{"id":"b","label":"Chim phượng"},{"id":"c","label":"Bát sứ ngự"},{"id":"d","label":"Chữ Quan"},{"id":"e","label":"Gạch hoa"},{"id":"f","label":"Lò quan"}]}',
 4),
('b3400001-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 'ST05', 'SPOT_DIFF',
 'Tìm khác biệt: hai lớp ký ức',
 '{"leftTitle":"Di sản cổ","rightTitle":"Không gian hiện đại","leftItems":["Nền móng khảo cổ","Ngói men trưng bày","Hầm chỉ huy quân sự","Rồng đá"],"rightItems":["Nền móng khảo cổ","Ngói men trưng bày","Không gian diễn giải hiện đại","Rồng đá"],"diffIndices":[2]}',
 5),
('b3400001-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: kết hành trình',
 '{"scenario":"Bạn đứng gần Kỳ đài, muốn chụp ảnh biểu tượng Cột cờ và kết thúc tour an toàn.","choices":[{"id":"n","label":"Hướng Kỳ đài / Cột cờ — điểm kết chính thức","correct":true},{"id":"e","label":"Vào khu đang đóng cửa khảo cổ","correct":false},{"id":"s","label":"Rời khu không check-out","correct":false},{"id":"w","label":"Leo tường di tích","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;

INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST01', 'guide', 'UNESCO?', 'Hoàng thành Thăng Long được UNESCO công nhận vì lý do gì?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST02', 'guide', '18 Hoàng Diệu', 'Khu khảo cổ 18 Hoàng Diệu cho thấy những tầng văn hóa nào?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST03', 'guide', 'Rồng đá', 'Đôi rồng đá Điện Kính Thiên có ý nghĩa gì?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST04', 'guide', 'Ngói men', 'Ngói men xanh thời Lý nói lên điều gì về cung đình?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST05', 'guide', 'Hầm chỉ huy', 'Hầm chỉ huy trong Hoàng thành gắn với giai đoạn lịch sử nào?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST06', 'guide', 'Kỳ đài', 'Cột cờ Hà Nội được xây dựng vào thời nào?', 1);

-- ========== Đại Nội Huế ==========
INSERT INTO stations (id, site_code, code, name, sort_order, lat, lng, quest_step_key, is_active) VALUES
('a3400011-0000-4000-8000-000000000001', 'dai-noi-hue', 'ST01', 'Ngọ Môn', 1, 16.469617, 107.579412, 'hue:ST01', TRUE),
('a3400011-0000-4000-8000-000000000002', 'dai-noi-hue', 'ST02', 'Điện Thái Hòa', 2, 16.470200, 107.579800, 'hue:ST02', TRUE),
('a3400011-0000-4000-8000-000000000003', 'dai-noi-hue', 'ST03', 'Thế Miếu', 3, 16.468900, 107.578500, 'hue:ST03', TRUE),
('a3400011-0000-4000-8000-000000000004', 'dai-noi-hue', 'ST04', 'Cung Kiến Trung', 4, 16.470800, 107.580200, 'hue:ST04', TRUE),
('a3400011-0000-4000-8000-000000000005', 'dai-noi-hue', 'ST05', 'Hồ Tịnh Tâm', 5, 16.471500, 107.578900, 'hue:ST05', TRUE),
('a3400011-0000-4000-8000-000000000006', 'dai-noi-hue', 'ST06', 'Lối kết hành trình', 6, 16.469000, 107.578000, 'hue:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3400012-0000-4000-8000-000000000001', 'a3400011-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu',
 'Ngọ Môn là cửa chính phía Nam của Hoàng thành Huế, gắn với Lầu Ngũ Phụng và các nghi lễ lớn triều Nguyễn.', 1),
('a3400012-0000-4000-8000-000000000002', 'a3400011-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu',
 'Điện Thái Hòa và sân Đại Triều Nghi là nơi đặt Ngai vàng — trung tâm nghi lễ đăng quang và triều hội.', 1),
('a3400012-0000-4000-8000-000000000003', 'a3400011-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu',
 'Thế Miếu thờ các vua triều Nguyễn — không gian tâm linh và ký ức dòng họ hoàng tộc.', 1),
('a3400012-0000-4000-8000-000000000004', 'a3400011-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu',
 'Cung Kiến Trung và khu vực cung cấm phản ánh đời sống nội cung — tuân thủ hướng dẫn BQL khi tham quan.', 1),
('a3400012-0000-4000-8000-000000000005', 'a3400011-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu',
 'Hồ Tịnh Tâm / khu vườn cung đình mang không khí thư thái giữa lòng kinh thành.', 1),
('a3400012-0000-4000-8000-000000000006', 'a3400011-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu',
 'Điểm kết hành trình: tổng kết sáu trạm Đại Nội và chia sẻ Hồ sơ giao liên.', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a3400013-0000-4000-8000-000000000001', 'dai-noi-hue', 1, 'ST01',
 'Một ngày trong Đại Nội — Chương 1: Qua Ngọ Môn',
 'Bạn bước qua cửa chính kinh thành, dưới bóng Lầu Ngũ Phụng — nơi từng diễn ra nghi lễ lớn của triều Nguyễn.',
 FALSE, 1),
('a3400013-0000-4000-8000-000000000002', 'dai-noi-hue', 2, 'ST02',
 'Một ngày trong Đại Nội — Chương 2: Trước Ngai vàng',
 'Sân Đại Triều Nghi và Điện Thái Hòa hé lộ nghi thức quyền lực — từ đăng quang đến triều hội ngày rằm.',
 FALSE, 2),
('a3400013-0000-4000-8000-000000000003', 'dai-noi-hue', 3, 'ST03',
 'Một ngày trong Đại Nội — Chương 3: Thế Miếu',
 'Không gian thờ các vua Nguyễn — bạn hiểu chữ hiếu và cách triều đại tự kể câu chuyện của mình.',
 TRUE, 3),
('a3400013-0000-4000-8000-000000000004', 'dai-noi-hue', 4, 'ST04',
 'Một ngày trong Đại Nội — Chương 4: Trong cung',
 'Khu cung cấm / Kiến Trung: đời sống nội cung và quy tắc tham quan hiện đại.',
 TRUE, 4),
('a3400013-0000-4000-8000-000000000005', 'dai-noi-hue', 5, 'ST05',
 'Một ngày trong Đại Nội — Chương 5: Bên hồ Tịnh Tâm',
 'Khoảng lặng giữa kinh thành — vườn và hồ kể về thẩm mỹ sống của cung đình.',
 TRUE, 5),
('a3400013-0000-4000-8000-000000000006', 'dai-noi-hue', 6, 'ST06',
 'Một ngày trong Đại Nội — Chương 6: Lối ra',
 'Kết thúc hành trình Đại Nội, bạn mang theo câu chuyện kinh thành Huế và mã giới thiệu.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b3400011-0000-4000-8000-000000000001', 'dai-noi-hue', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm: Ngọ Môn',
 '{"timeLimitSec":90,"questions":[{"prompt":"Ngọ Môn nằm ở hướng nào của Hoàng thành Huế?","options":["Phía Nam","Phía Bắc","Phía Đông"],"correctIndex":0},{"prompt":"Phía trên Ngọ Môn là công trình nào?","options":["Lầu Ngũ Phụng","Chùa Thiên Mụ","Kỳ đài"],"correctIndex":0},{"prompt":"Ngọ Môn gắn với triều đại nào?","options":["Nguyễn","Lý","Trần"],"correctIndex":0}]}',
 1),
('b3400011-0000-4000-8000-000000000002', 'dai-noi-hue', 'ST02', 'TIMELINE_ORDER',
 'Sắp thứ tự: nghi lễ triều Nguyễn',
 '{"items":["Vua vào Ngọ Môn","Triều thần tập trung sân Đại Triều","Nghi thức tại Điện Thái Hòa","Ban bố chiếu chỉ"],"hint":"Theo trình tự buổi đại triều điển hình."}',
 2),
('b3400011-0000-4000-8000-000000000003', 'dai-noi-hue', 'ST03', 'MATCH_TERMS',
 'Nối thuật ngữ: Thế Miếu',
 '{"terms":["Thế Miếu","Triều Nguyễn","Ngai vàng","Trùng thiềm điệp ốc"],"definitions":["Nơi thờ các vua","Triều đại kinh đô Huế","Biểu tượng quyền lực tại Thái Hòa","Kiểu nhà nối nhà đặc trưng Huế"],"answerKey":[0,1,2,3]}',
 3),
('b3400011-0000-4000-8000-000000000004', 'dai-noi-hue', 'ST04', 'MEMORY_PAIRS',
 'Ghép cặp: cung đình Huế',
 '{"pairs":[{"id":"a","label":"Cung Kiến Trung"},{"id":"b","label":"Nội cung"},{"id":"c","label":"Tả Vu"},{"id":"d","label":"Hữu Vu"},{"id":"e","label":"Hướng dẫn viên"},{"id":"f","label":"Quy tắc BQL"}]}',
 4),
('b3400011-0000-4000-8000-000000000005', 'dai-noi-hue', 'ST05', 'SPOT_DIFF',
 'Tìm khác biệt: không gian vườn hồ',
 '{"leftTitle":"Buổi sáng","rightTitle":"Buổi chiều","leftItems":["Hồ nước tĩnh","Bóng cây dài","Đường đi bộ mở","Nhóm đông"],"rightItems":["Hồ nước tĩnh","Bóng cây ngắn hơn","Đường đi bộ mở","Ít người hơn"],"diffIndices":[1,3]}',
 5),
('b3400011-0000-4000-8000-000000000006', 'dai-noi-hue', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: kết thúc tour',
 '{"scenario":"Bạn đã đủ sáu trạm Đại Nội và muốn ra cổng an toàn để mở Wrapped.","choices":[{"id":"s","label":"Theo lối kết / cổng phụ được BQL chỉ dẫn","correct":true},{"id":"n","label":"Leo tường thành","correct":false},{"id":"e","label":"Vào khu đang tu bổ cấm vào","correct":false},{"id":"w","label":"Ở lại sau giờ đóng cửa","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;

INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(gen_random_uuid(), 'dai-noi-hue', 'ST01', 'guide', 'Ngọ Môn', 'Ngọ Môn dùng cho nghi lễ gì dưới triều Nguyễn?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST02', 'guide', 'Thái Hòa', 'Điện Thái Hòa có vai trò gì trong kinh thành?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST03', 'guide', 'Thế Miếu', 'Thế Miếu thờ những ai?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST04', 'guide', 'Cung cấm', 'Khi tham quan khu cung cần lưu ý gì?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST05', 'guide', 'Tịnh Tâm', 'Hồ Tịnh Tâm mang ý nghĩa gì trong cảnh quan cung đình?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST06', 'guide', 'Kết tour', 'Sau sáu trạm Đại Nội nên làm gì tiếp theo trong TimeLens?', 1);

-- END migration: V34__pilot_httl_hue_stations.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V35__rag_site_code.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V35__rag_site_code.sql
-- ============================================================================
-- V35: Scope RAG retrieval by site so ST01 at Hue does not pull Cu Chi chunks.

ALTER TABLE rag_sources
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

ALTER TABLE rag_chunks
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

-- Existing Cu Chi corpus rows (station-tagged) default to cu-chi
UPDATE rag_sources SET site_code = 'cu-chi'
WHERE site_code IS NULL AND station_code IS NOT NULL;

UPDATE rag_chunks SET site_code = 'cu-chi'
WHERE site_code IS NULL AND station_code IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_rag_sources_site_station ON rag_sources (site_code, station_code);
CREATE INDEX IF NOT EXISTS idx_rag_chunks_site_station ON rag_chunks (site_code, station_code);

-- END migration: V35__rag_site_code.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V36__b2b2c_interest_site.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V36__b2b2c_interest_site.sql
-- ============================================================================
-- B2B2C: which pilot site the lead is interested in (cu-chi | hoang-thanh-thang-long | dai-noi-hue)
ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS interest_site_code VARCHAR(64) NULL;

CREATE INDEX IF NOT EXISTS idx_heritage_inq_interest_site
    ON heritage_digitization_inquiries (interest_site_code);

-- END migration: V36__b2b2c_interest_site.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V37__password_reset_otp.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V37__password_reset_otp.sql
-- ============================================================================
-- Password reset via email OTP (local email/password accounts)

CREATE TABLE IF NOT EXISTS password_reset_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    otp_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified_at TIMESTAMPTZ,
    reset_token_hash VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_password_reset_challenges_user_active
    ON password_reset_challenges(user_id, created_at DESC)
    WHERE consumed_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_challenges_reset_token
    ON password_reset_challenges(reset_token_hash)
    WHERE reset_token_hash IS NOT NULL;

-- END migration: V37__password_reset_otp.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V38__chrono_quest_prompts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V38__chrono_quest_prompts.sql
-- ============================================================================
UPDATE quest_steps
SET title = 'Chương 1 · Hỏi Chrono về các tầng',
    objective = 'Nghe Chrono kể cấu trúc 3 tầng và độ sâu tầng sâu nhất.',
    description = 'Tầng sâu nhất sâu 8–10 m, tối đa 12 m.',
    hint = 'Gửi câu hỏi đã soạn. Chrono kể theo tư liệu.',
    action_label = 'Hỏi Chrono',
    chat_prompt = 'Chrono ơi, địa đạo Củ Chi có mấy tầng, tầng sâu nhất sâu bao nhiêu?'
WHERE unlock_key = 'dialogue:anh-ba'
  AND quest_id = '33333333-3333-3333-3333-333333333334';

UPDATE quest_steps
SET title = 'Chương 1 · Hỏi Chrono về bếp Hoàng Cầm',
    objective = 'Nghe Chrono kể cách Bếp Hoàng Cầm giấu khói.',
    description = 'Khói đi theo rãnh, lọc qua đất ẩm, thoát lên chỉ còn hơi như sương sớm.',
    hint = 'Gửi câu hỏi đã soạn cho Chrono.',
    action_label = 'Hỏi Chrono',
    chat_prompt = 'Chrono ơi, Bếp Hoàng Cầm dẫn khói theo rãnh và lọc qua đất ẩm như thế nào?'
WHERE unlock_key = 'dialogue:chi-nam'
  AND quest_id = '33333333-3333-3333-3333-333333333335';

-- END migration: V38__chrono_quest_prompts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V39__restore_vietnamese_quest_text.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V39__restore_vietnamese_quest_text.sql
-- ============================================================================
-- Seed cũ bị ghi vào database bằng kết nối không phải UTF-8, dấu tiếng Việt thành dấu hỏi thật.
UPDATE locations
SET name = 'Địa đạo Củ Chi',
    description = 'Khu di tích lịch sử Địa đạo Củ Chi — hệ thống hầm ngầm thời kháng chiến tại huyện Củ Chi, TP.HCM.'
WHERE id = '11111111-1111-1111-1111-111111111111'
  AND (name LIKE '%?%' OR description LIKE '%?%');

UPDATE quests
SET title = 'Hành trình dưới lòng đất',
    description = 'Khám phá Bếp Hoàng Cầm, Phòng họp và Time Portal 1948 — sau đó check-in tại khu di tích Củ Chi.',
    story = 'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.'
WHERE id = '33333333-3333-3333-3333-333333333333'
  AND title LIKE '%?%';

UPDATE quests
SET title = 'Mật mã Sa bàn Tác chiến',
    description = 'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.',
    story = 'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".'
WHERE id = '33333333-3333-3333-3333-333333333334'
  AND title LIKE '%?%';

UPDATE quests
SET title = 'Ánh sáng Đèn Mù U',
    description = 'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.',
    story = 'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.'
WHERE id = '33333333-3333-3333-3333-333333333335'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 2 · Khảo sát Bẫy Chông',
    objective = 'Vào tour 360° tìm hiểu mô hình hầm chông tự tạo.',
    description = 'Những chiếc bẫy tre thô sơ nhưng là nỗi khiếp sợ của quân thù.',
    hint = 'Trong tour 360°, tìm điểm nóng "Khu vực Bẫy Chông".',
    action_label = 'Vào Tour 360°'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'tour:sa-ban'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 3 · Phục kích Xe Tăng',
    objective = 'Mở Time Portal xem cảnh phục kích xe tăng M41.',
    description = 'Tái hiện khoảnh khắc quân ta dùng chiến thuật du kích phá hủy xe tăng.',
    hint = 'Mở Portal chọn mốc thời gian 1968.',
    action_label = 'Mở Time Portal'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'era:1968'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 2 · Trạm xá Dã chiến',
    objective = 'Tìm kiếm trạm y tế dưới lòng đất trong Tour 360°.',
    description = 'Nơi các bác sĩ quân y cứu chữa thương binh trong điều kiện thiếu thốn.',
    hint = 'Khám phá khu vực Trạm xá trong sa bàn 360°.',
    action_label = 'Vào Tour 360°'
WHERE quest_id = '33333333-3333-3333-3333-333333333335'
  AND unlock_key = 'tour:tram-xa'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 3 · Lớp học bình dân',
    objective = 'Mở Time Portal chứng kiến lớp học dưới ánh đèn mù u.',
    description = 'Sự kiên cường của những đứa trẻ học chữ dưới làn bom đạn.',
    hint = 'Mở Portal mốc 1968 tại khu vực Lớp học.',
    action_label = 'Mở Time Portal'
WHERE quest_id = '33333333-3333-3333-3333-333333333335'
  AND unlock_key = 'era:1968'
  AND title LIKE '%?%';

-- END migration: V39__restore_vietnamese_quest_text.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V40__cu_chi_quest_discovery_points.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V40__cu_chi_quest_discovery_points.sql
-- ============================================================================
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000030', '11111111-1111-1111-1111-111111111111', 'Hỏi Chrono về các tầng', 30.00, 35.00, 'dialogue:anh-ba', 30),
('d0000001-0000-4000-8000-000000000031', '11111111-1111-1111-1111-111111111111', 'Hỏi Chrono về bếp Hoàng Cầm', 30.00, 35.00, 'dialogue:chi-nam', 31),
('d0000001-0000-4000-8000-000000000032', '11111111-1111-1111-1111-111111111111', 'Mảnh sa bàn', 36.00, 42.00, 'tour:sa-ban', 32),
('d0000001-0000-4000-8000-000000000033', '11111111-1111-1111-1111-111111111111', 'Tour trạm xá', 50.00, 50.00, 'tour:tram-xa', 33)
ON CONFLICT (location_id, unlock_key) DO NOTHING;

UPDATE quest_steps
SET objective = 'Tìm mảnh giấu gần nhà sa bàn trên sơ đồ, rồi đưa mã cho Chrono.',
    description = 'Đứng trong tour không đủ. Mảnh nằm gần nhà biểu diễn sa bàn.',
    hint = 'Tìm mảnh gần nhà sa bàn, dán mã cho Chrono.',
    action_label = 'Tìm mảnh trên sơ đồ'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'tour:sa-ban';

-- END migration: V40__cu_chi_quest_discovery_points.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V41__quest1_journey_steps.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V41__quest1_journey_steps.sql
-- ============================================================================
-- Mật lệnh Hành trình dưới lòng đất: bốn chương UI khớp ba khóa đã có.
-- Không thêm khóa vào step_discovery_keys. Chương 4 là check-in, không phải khóa khám phá.
-- ON CONFLICT DO NOTHING nên tài khoản đã phá đảo không bị mở lại và không nhận thưởng lần hai.

ALTER TABLE quests ADD COLUMN IF NOT EXISTS step_discovery_keys TEXT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS completion_trigger VARCHAR(32);
ALTER TABLE quests ADD COLUMN IF NOT EXISTS steps_total INT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS cover_image TEXT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS require_onsite_checkin BOOLEAN NOT NULL DEFAULT false;

CREATE UNIQUE INDEX IF NOT EXISTS uq_quest_steps_quest_unlock
    ON quest_steps (quest_id, unlock_key);

INSERT INTO quests (
    id, location_id, title, description, story, points_reward, required_order,
    step_discovery_keys, completion_trigger, steps_total, cover_image, require_onsite_checkin
)
SELECT
    '33333333-3333-3333-3333-333333333333',
    '11111111-1111-1111-1111-111111111111',
    'Hành trình dưới lòng đất',
    'Khám phá Bếp Hoàng Cầm, Phòng họp và Time Portal 1948 — sau đó check-in tại khu di tích Củ Chi.',
    'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.',
    100,
    1,
    'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223,era:1948',
    'checkin',
    4,
    '/media/heritage/dia-dao-cu-chi/cover.jpeg',
    true
WHERE EXISTS (
    SELECT 1 FROM locations WHERE id = '11111111-1111-1111-1111-111111111111'
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_steps (
    id, quest_id, step_order, unlock_key, title, objective, description, hint,
    action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image
)
SELECT * FROM (VALUES
    (
        '33333333-3333-3333-3333-333333333301'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        1,
        'scene:22222222-2222-2222-2222-222222222221',
        'Chương 1 · Bếp Hoàng Cầm',
        'Ở lại cảnh Bếp Hoàng Cầm trong tour.',
        'Điểm chương trên thẻ chỉ là nhãn. Thưởng phá đảo cộng một lần khi check-in sau đủ ba khóa.',
        'Mở tour và đứng tại cảnh bếp.',
        'tour',
        'Vào tour',
        25,
        NULL,
        NULL::int,
        '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg'
    ),
    (
        '33333333-3333-3333-3333-333333333302'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        2,
        'scene:22222222-2222-2222-2222-222222222223',
        'Chương 2 · Phòng họp',
        'Ở lại cảnh phòng họp dưới lòng đất.',
        'Khóa này đã có trên hồ sơ thì chương được đánh dấu, không ghi điểm lần hai.',
        'Mở tour và đứng tại phòng họp.',
        'tour',
        'Vào tour',
        25,
        NULL,
        NULL::int,
        '/media/cu-chi/scenes/phong-hop-2026.jpg'
    ),
    (
        '33333333-3333-3333-3333-333333333303'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        3,
        'era:1948',
        'Chương 3 · Cổng 1948',
        'Mở cổng thời gian mốc 1948.',
        'Cổng 1948 vẫn đi qua Premium.',
        'Kéo tới mốc 1948.',
        'portal',
        'Mở cổng 1948',
        25,
        NULL,
        1948,
        '/media/heritage/dia-dao-cu-chi/cover.jpeg'
    ),
    (
        '33333333-3333-3333-3333-333333333304'::uuid,
        '33333333-3333-3333-3333-333333333333'::uuid,
        4,
        'checkin:hanh-trinh-duoi-long-dat',
        'Chương 4 · Check-in',
        'Check-in tại khu di tích sau khi đủ ba khóa.',
        'Phá đảo cần đủ ba khóa khám phá rồi check-in. Khóa check-in này không nằm trong step_discovery_keys.',
        'Mở màn quét mã khi đang ở khu di tích.',
        'checkin',
        'Check-in',
        25,
        NULL,
        NULL::int,
        '/media/heritage/dia-dao-cu-chi/cover.jpeg'
    )
) AS rows(id, quest_id, step_order, unlock_key, title, objective, description, hint, action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image)
WHERE EXISTS (
    SELECT 1 FROM quests WHERE id = '33333333-3333-3333-3333-333333333333'
)
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

-- END migration: V41__quest1_journey_steps.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V42__quest_minigames.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V42__quest_minigames.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS quest_minigames (
    id          UUID PRIMARY KEY,
    engine      VARCHAR(32) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    config      JSONB NOT NULL,
    CONSTRAINT quest_minigames_engine_chk CHECK (
        engine IN ('xep_tang', 'soi_nap', 'dao_gap', 'san_co_vat')
    )
);

CREATE TABLE IF NOT EXISTS user_game_results (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    game_id       UUID NOT NULL REFERENCES quest_minigames(id) ON DELETE CASCADE,
    best_stars    INT NOT NULL DEFAULT 0,
    attempts      INT NOT NULL DEFAULT 0,
    last_level_id VARCHAR(64),
    CONSTRAINT user_game_results_user_game_uq UNIQUE (user_id, game_id),
    CONSTRAINT user_game_results_stars_chk CHECK (best_stars BETWEEN 0 AND 3)
);

ALTER TABLE quest_steps
    ADD COLUMN IF NOT EXISTS minigame_id UUID REFERENCES quest_minigames(id);

-- END migration: V42__quest_minigames.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V43__quest_minigame_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V43__quest_minigame_seed.sql
-- ============================================================================
-- Seed bốn mini game và một mật lệnh thử, chỉ hiện trên bản dev của web.
-- Đáp án nằm trong config. GET không trả các trường này.

INSERT INTO quest_minigames (id, engine, title, config) VALUES
(
    'a1000001-0000-4000-8000-000000000001',
    'xep_tang',
    'Xếp tầng',
    $json${
      "facts": [
        {"text": "Tầng 1 chịu được đạn pháo và sức nặng của xe tăng.", "url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html"},
        {"text": "Tầng 2 chống được bom cỡ nhỏ.", "url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html"},
        {"text": "Tầng 3 chống được hầu hết các loại bom đạn.", "url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html"}
      ],
      "levels": [
        {
          "id": "fn-1",
          "prompt": "Kéo mỗi tác dụng vào đúng tầng.",
          "floors": [{"id": "t1", "label": "Tầng 1"}, {"id": "t2", "label": "Tầng 2"}, {"id": "t3", "label": "Tầng 3"}],
          "items": [
            {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
            {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"},
            {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"}
          ]
        },
        {
          "id": "fn-2",
          "prompt": "Ghép lại, thứ tự thẻ đã đổi.",
          "floors": [{"id": "t1", "label": "Tầng 1"}, {"id": "t2", "label": "Tầng 2"}, {"id": "t3", "label": "Tầng 3"}],
          "items": [
            {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"},
            {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
            {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"}
          ]
        },
        {
          "id": "fn-3",
          "prompt": "Một lượt nữa, cách gọi ngắn hơn.",
          "floors": [{"id": "t1", "label": "Tầng 1"}, {"id": "t2", "label": "Tầng 2"}, {"id": "t3", "label": "Tầng 3"}],
          "items": [
            {"id": "small", "label": "Bom cỡ nhỏ", "floorId": "t2"},
            {"id": "most", "label": "Hầu hết bom đạn", "floorId": "t3"},
            {"id": "tank", "label": "Đạn pháo và xe tăng", "floorId": "t1"}
          ]
        }
      ]
    }$json$::jsonb
),
(
    'a1000001-0000-4000-8000-000000000002',
    'soi_nap',
    'Soi nắp hầm',
    $json${
      "facts": [
        {"text": "Chỗ lên xuống giữa các tầng có nắp hầm bí mật, cửa lên được ngụy trang.", "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"}
      ],
      "levels": [
        {"id": "lid-1", "prompt": "Chạm cửa ngụy trang trong 10 giây.", "imageUrl": "/minigames/soi-nap-1.svg", "targetX": 28, "targetY": 62, "radius": 8},
        {"id": "lid-2", "prompt": "Cửa nằm chỗ khác. Ba lần chạm hụt là bị lộ.", "imageUrl": "/minigames/soi-nap-2.svg", "targetX": 72, "targetY": 38, "radius": 8},
        {"id": "lid-3", "prompt": "Tìm nắp trước khi hết giờ.", "imageUrl": "/minigames/soi-nap-3.svg", "targetX": 46, "targetY": 24, "radius": 8}
      ]
    }$json$::jsonb
),
(
    'a1000001-0000-4000-8000-000000000003',
    'dao_gap',
    'Đào gặp nhau',
    $json${
      "facts": [
        {"text": "Hai giếng cách nhau khoảng 6–15 m, đào xuống khoảng 3–4 m rồi hai người đào hướng vào nhau.", "url": "https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html"}
      ],
      "levels": [
        {"id": "well-1", "prompt": "Chỉnh hướng để hai giếng gặp nhau.", "angleMin": -60, "angleMax": 60, "targetAngle": -28, "tolerance": 8},
        {"id": "well-2", "prompt": "Giếng này lệch hướng khác.", "angleMin": -60, "angleMax": 60, "targetAngle": 16, "tolerance": 8},
        {"id": "well-3", "prompt": "Nghe tiếng cuốc rồi chỉnh lại.", "angleMin": -60, "angleMax": 60, "targetAngle": 44, "tolerance": 8}
      ]
    }$json$::jsonb
),
(
    'a1000001-0000-4000-8000-000000000004',
    'san_co_vat',
    'Săn cổ vật',
    $json${
      "facts": [
        {"text": "Trong địa đạo có giếng nước và bếp Hoàng Cầm.", "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"}
      ],
      "levels": [
        {
          "id": "ar-bep",
          "prompt": "Chạm hai chỗ giấu trên ảnh bếp.",
          "imageUrl": "/media/cu-chi/scenes/bep-hoang-cam-2026.jpg",
          "timeLimitMs": 20000,
          "radius": 8,
          "hotspots": [{"x": 22, "y": 70}, {"x": 68, "y": 36}]
        },
        {
          "id": "ar-hop",
          "prompt": "Ba chỗ giấu trong phòng họp.",
          "imageUrl": "/media/cu-chi/scenes/phong-hop-2026.jpg",
          "timeLimitMs": 25000,
          "radius": 8,
          "hotspots": [{"x": 18, "y": 40}, {"x": 50, "y": 72}, {"x": 78, "y": 28}]
        },
        {
          "id": "ar-gieng",
          "prompt": "Hai chỗ giấu cạnh giếng.",
          "imageUrl": "/media/cu-chi/scenes/gieng-2026.jpg",
          "timeLimitMs": 20000,
          "radius": 8,
          "hotspots": [{"x": 34, "y": 58}, {"x": 74, "y": 30}]
        }
      ]
    }$json$::jsonb
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000041', '11111111-1111-1111-1111-111111111111', 'Xếp tầng', 20, 20, 'minigame:xep-tang', 41),
('d0000001-0000-4000-8000-000000000042', '11111111-1111-1111-1111-111111111111', 'Soi nắp hầm', 30, 20, 'minigame:soi-nap', 42),
('d0000001-0000-4000-8000-000000000043', '11111111-1111-1111-1111-111111111111', 'Đào gặp nhau', 40, 20, 'minigame:dao-gap', 43),
('d0000001-0000-4000-8000-000000000044', '11111111-1111-1111-1111-111111111111', 'Săn cổ vật', 50, 20, 'minigame:san-co-vat', 44)
ON CONFLICT (location_id, unlock_key) DO NOTHING;

INSERT INTO quests (
    id, location_id, title, description, story, points_reward, required_order,
    step_discovery_keys, completion_trigger, steps_total, cover_image, require_onsite_checkin
)
SELECT
    '33333333-3333-3333-3333-333333333336',
    '11111111-1111-1111-1111-111111111111',
    'Mật lệnh thử game',
    'Bốn mini game để kiểm tra chấm sao. Chỉ hiện khi chạy web dev.',
    'Không tính vào thứ tự ba mật lệnh Củ Chi.',
    0,
    NULL,
    'minigame:xep-tang,minigame:soi-nap,minigame:dao-gap,minigame:san-co-vat',
    'discovery',
    4,
    '/media/heritage/dia-dao-cu-chi/cover.jpeg',
    false
WHERE EXISTS (SELECT 1 FROM locations WHERE id = '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_steps (
    id, quest_id, step_order, unlock_key, title, objective, description, hint,
    action_type, action_label, xp_partial, portal_era, preview_image, minigame_id
)
SELECT * FROM (VALUES
    ('33333333-3333-3333-3333-333333333361'::uuid, '33333333-3333-3333-3333-333333333336'::uuid, 1, 'minigame:xep-tang', 'Chương 1 · Xếp tầng', 'Ghép tác dụng với tầng.', 'Lần thắng đầu ghi khóa khám phá.', 'Kéo thẻ vào tầng.', 'minigame', 'Chơi', 0, NULL::int, '/media/cu-chi/scenes/phong-hop-2026.jpg', 'a1000001-0000-4000-8000-000000000001'::uuid),
    ('33333333-3333-3333-3333-333333333362'::uuid, '33333333-3333-3333-3333-333333333336'::uuid, 2, 'minigame:soi-nap', 'Chương 2 · Soi nắp hầm', 'Tìm cửa ngụy trang.', 'Ba lần chạm hụt là bị lộ.', 'Chạm vào hình.', 'minigame', 'Chơi', 0, NULL::int, '/minigames/soi-nap-1.svg', 'a1000001-0000-4000-8000-000000000002'::uuid),
    ('33333333-3333-3333-3333-333333333363'::uuid, '33333333-3333-3333-3333-333333333336'::uuid, 3, 'minigame:dao-gap', 'Chương 3 · Đào gặp nhau', 'Chỉnh hướng hai giếng.', 'Khoảng cách giếng không phải độ sâu tầng.', 'Gửi hướng.', 'minigame', 'Chơi', 0, NULL::int, '/media/cu-chi/scenes/gieng-2026.jpg', 'a1000001-0000-4000-8000-000000000003'::uuid),
    ('33333333-3333-3333-3333-333333333364'::uuid, '33333333-3333-3333-3333-333333333336'::uuid, 4, 'minigame:san-co-vat', 'Chương 4 · Săn cổ vật', 'Chạm hotspot trên ảnh.', 'Tái dùng ảnh cảnh Củ Chi.', 'Tìm đủ chỗ giấu.', 'minigame', 'Chơi', 0, NULL::int, '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', 'a1000001-0000-4000-8000-000000000004'::uuid)
) AS rows(id, quest_id, step_order, unlock_key, title, objective, description, hint, action_type, action_label, xp_partial, portal_era, preview_image, minigame_id)
WHERE EXISTS (SELECT 1 FROM quests WHERE id = '33333333-3333-3333-3333-333333333336')
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

-- END migration: V43__quest_minigame_seed.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V44__phase2_quests_collections.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V44__phase2_quests_collections.sql
-- ============================================================================
-- Chạy tay trên Windows: đặt PGCLIENTENCODING=UTF8 trước khi gọi psql.
-- Flyway đọc file này bằng spring.flyway.encoding=UTF-8.
-- Mật lệnh 4–6 không dùng required_order. Chúng mở khi đã có hai khóa tour của mật lệnh 1.
-- Không thêm quest_steps cho chín di tích.

ALTER TABLE quests ADD COLUMN IF NOT EXISTS unlock_after_quest_id UUID;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS unlock_discovery_keys TEXT;

ALTER TABLE quests DROP CONSTRAINT IF EXISTS quests_unlock_after_quest_id_fkey;
ALTER TABLE quests
    ADD CONSTRAINT quests_unlock_after_quest_id_fkey
    FOREIGN KEY (unlock_after_quest_id) REFERENCES quests(id);

ALTER TABLE quest_minigames DROP CONSTRAINT IF EXISTS quest_minigames_engine_chk;
ALTER TABLE quest_minigames ADD CONSTRAINT quest_minigames_engine_chk CHECK (
    engine IN ('xep_tang', 'soi_nap', 'dao_gap', 'san_co_vat', 'duong_khoi', 'im_lang', 'phong_van')
);

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS collection_name VARCHAR(64);
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS rarity VARCHAR(16);
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS source_url TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS image_source TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS image_license TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS fact_verified BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS unlock_minigame_id UUID;

ALTER TABLE artifacts DROP CONSTRAINT IF EXISTS artifacts_rarity_chk;
ALTER TABLE artifacts ADD CONSTRAINT artifacts_rarity_chk CHECK (
    rarity IS NULL OR rarity IN ('common', 'rare', 'epic')
);

UPDATE quest_minigames
SET title = 'Đếm dấu',
    config = $json$
{
      "facts": [
        {"text": "Miệng hầm được ngụy trang để lẫn với mặt đất.", "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"}
      ],
      "levels": [
        {"id": "dem-1", "prompt": "Đếm số dấu trên hình. Không chạm tìm.", "imageUrl": "/minigames/dem-dau-1.svg", "answerCount": 4},
        {"id": "dem-2", "prompt": "Hình khác, đếm lại.", "imageUrl": "/minigames/dem-dau-2.svg", "answerCount": 7},
        {"id": "dem-3", "prompt": "Lần cuối.", "imageUrl": "/minigames/dem-dau-3.svg", "answerCount": 5}
      ]
    }$json$::jsonb
WHERE id = 'a1000001-0000-4000-8000-000000000004';

UPDATE quest_steps
SET title = 'Chương 4 · Đếm dấu',
    objective = 'Đếm số dấu trên hình.',
    description = 'Luật đếm, không chạm tìm như Soi nắp.',
    hint = 'Nhập một số.'
WHERE id = '33333333-3333-3333-3333-333333333364';

INSERT INTO quest_minigames (id, engine, title, config)
SELECT 'a1000001-0000-4000-8000-000000000012', engine, 'Soi nắp', config
FROM quest_minigames
WHERE id = 'a1000001-0000-4000-8000-000000000002'
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_minigames (id, engine, title, config)
SELECT 'a1000001-0000-4000-8000-000000000013', engine, 'Xếp tầng', config
FROM quest_minigames
WHERE id = 'a1000001-0000-4000-8000-000000000001'
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_minigames (id, engine, title, config) VALUES
(
    'a1000001-0000-4000-8000-000000000011',
    'duong_khoi',
    'Đường khói',
    $json$
{
      "facts": [
        {"text": "Khói đi qua đường rãnh, tản ra, trên mặt đất còn làn khói mỏng bay là là.", "url": "https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm"}
      ],
      "levels": [
        {
          "id": "dk-1",
          "prompt": "Xoay từng đoạn cho khói đi hết rãnh.",
          "cells": [
            {"id": "a", "kind": "straight", "rotation": 1},
            {"id": "b", "kind": "elbow", "rotation": 2},
            {"id": "c", "kind": "straight", "rotation": 0}
          ]
        },
        {
          "id": "dk-2",
          "prompt": "Rãnh đổi hướng.",
          "cells": [
            {"id": "a", "kind": "elbow", "rotation": 1},
            {"id": "b", "kind": "straight", "rotation": 0},
            {"id": "c", "kind": "elbow", "rotation": 3}
          ]
        },
        {
          "id": "dk-3",
          "prompt": "Rãnh dài hơn.",
          "cells": [
            {"id": "a", "kind": "straight", "rotation": 1},
            {"id": "b", "kind": "elbow", "rotation": 2},
            {"id": "c", "kind": "straight", "rotation": 1},
            {"id": "d", "kind": "elbow", "rotation": 0}
          ]
        }
      ]
    }$json$::jsonb
),
(
    'a1000001-0000-4000-8000-000000000014',
    'im_lang',
    'Im lặng',
    $json$
{
      "facts": [],
      "levels": [
        {"id": "im-1", "prompt": "Giữ im sau vài giây đo tiếng nền, hoặc giữ ngón tay. Không có dữ kiện lịch sử.", "holdMs": 3000, "marginRatio": 1.8},
        {"id": "im-2", "prompt": "Ngưỡng chặt hơn.", "holdMs": 3000, "marginRatio": 1.5},
        {"id": "im-3", "prompt": "Giữ lâu hơn.", "holdMs": 4000, "marginRatio": 1.5}
      ]
    }$json$::jsonb
),
(
    'a1000001-0000-4000-8000-000000000015',
    'phong_van',
    'Phỏng vấn nhân chứng',
    $json$
{
      "facts": [],
      "levels": [
        {
          "id": "pv-1",
          "prompt": "Nhắc đủ ba chủ đề. Có thể gõ không dấu.",
          "topics": [
            {"label": "Bếp và khói", "keywords": ["bếp", "khói", "Hoàng Cầm"]},
            {"label": "Nắp hầm", "keywords": ["nắp", "ngụy trang"]},
            {"label": "Ba tầng", "keywords": ["tầng", "địa đạo"]}
          ]
        }
      ]
    }$json$::jsonb
)
ON CONFLICT (id) DO NOTHING;

INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000045', '11111111-1111-1111-1111-111111111111', 'Hỏi khói bếp', 22, 30, 'dialogue:bep-khong-khoi', 45),
('d0000001-0000-4000-8000-000000000046', '11111111-1111-1111-1111-111111111111', 'Đường khói', 28, 30, 'minigame:duong-khoi', 46),
('d0000001-0000-4000-8000-000000000047', '11111111-1111-1111-1111-111111111111', 'Hỏi nắp hầm', 34, 30, 'dialogue:nap-ham-cu-chi', 47),
('d0000001-0000-4000-8000-000000000048', '11111111-1111-1111-1111-111111111111', 'Soi nắp', 40, 30, 'minigame:soi-nap-cu-chi', 48),
('d0000001-0000-4000-8000-000000000049', '11111111-1111-1111-1111-111111111111', 'Im lặng', 46, 30, 'minigame:im-lang', 49),
('d0000001-0000-4000-8000-000000000050', '11111111-1111-1111-1111-111111111111', 'Hỏi ba tầng', 52, 30, 'dialogue:ba-tang-long-dat', 50),
('d0000001-0000-4000-8000-000000000051', '11111111-1111-1111-1111-111111111111', 'Xếp tầng', 58, 30, 'minigame:xep-tang-cu-chi', 51)
ON CONFLICT (location_id, unlock_key) DO NOTHING;

INSERT INTO quests (
    id, location_id, title, description, story, points_reward, required_order,
    step_discovery_keys, completion_trigger, steps_total, cover_image, require_onsite_checkin,
    unlock_after_quest_id, unlock_discovery_keys
)
SELECT *
FROM (VALUES
    (
        '33333333-3333-3333-3333-333333333337'::uuid,
        '11111111-1111-1111-1111-111111111111'::uuid,
        'Bếp không khói',
        'Hỏi Chrono về khói bếp, rồi nối đường khói.',
        'Khói đi qua đường rãnh, tản ra, trên mặt đất còn làn khói mỏng.',
        100,
        NULL::int,
        'dialogue:bep-khong-khoi,minigame:duong-khoi',
        'discovery',
        2,
        '/media/heritage/dia-dao-cu-chi/cover.jpeg',
        false,
        '33333333-3333-3333-3333-333333333333'::uuid,
        'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223'
    ),
    (
        '33333333-3333-3333-3333-333333333338'::uuid,
        '11111111-1111-1111-1111-111111111111'::uuid,
        'Nắp hầm và cạm bẫy',
        'Hỏi Chrono về nắp hầm, soi nắp, rồi giữ im.',
        'Miệng hầm được ngụy trang. Im lặng không gắn dữ kiện lịch sử.',
        100,
        NULL::int,
        'dialogue:nap-ham-cu-chi,minigame:soi-nap-cu-chi,minigame:im-lang',
        'discovery',
        3,
        '/media/heritage/dia-dao-cu-chi/cover.jpeg',
        false,
        '33333333-3333-3333-3333-333333333333'::uuid,
        'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223'
    ),
    (
        '33333333-3333-3333-3333-333333333339'::uuid,
        '11111111-1111-1111-1111-111111111111'::uuid,
        'Ba tầng lòng đất',
        'Hỏi Chrono về ba tầng, rồi xếp đúng tác dụng.',
        'Tầng 1 chịu đạn pháo và xe tăng. Tầng 2 chống bom cỡ nhỏ. Tầng 3 chống hầu hết bom đạn.',
        100,
        NULL::int,
        'dialogue:ba-tang-long-dat,minigame:xep-tang-cu-chi',
        'discovery',
        2,
        '/media/heritage/dia-dao-cu-chi/cover.jpeg',
        false,
        '33333333-3333-3333-3333-333333333333'::uuid,
        'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223'
    )
) AS rows(
    id, location_id, title, description, story, points_reward, required_order,
    step_discovery_keys, completion_trigger, steps_total, cover_image, require_onsite_checkin,
    unlock_after_quest_id, unlock_discovery_keys
)
WHERE EXISTS (SELECT 1 FROM locations WHERE id = '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

INSERT INTO quest_steps (
    id, quest_id, step_order, unlock_key, title, objective, description, hint,
    action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image, minigame_id
)
SELECT * FROM (VALUES
    ('33333333-3333-3333-3333-333333333371'::uuid, '33333333-3333-3333-3333-333333333337'::uuid, 1, 'dialogue:bep-khong-khoi', 'Hỏi Chrono', 'Hỏi khói bếp đi đâu.', 'Gõ, nói hoặc gọi đều ghi chương.', 'Nhắc đường rãnh và làn khói.', 'dialogue', 'Hỏi Chrono', 0, 'Khói bếp Hoàng Cầm đi qua đâu trước khi ra mặt đất?', NULL::int, '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', NULL::uuid),
    ('33333333-3333-3333-3333-333333333372'::uuid, '33333333-3333-3333-3333-333333333337'::uuid, 2, 'minigame:duong-khoi', 'Đường khói', 'Nối rãnh cho khói đi hết.', 'Ba mức xoay ống.', 'Xoay rồi chấm.', 'minigame', 'Chơi', 0, NULL::text, NULL::int, '/minigames/dem-dau-1.svg', 'a1000001-0000-4000-8000-000000000011'::uuid),
    ('33333333-3333-3333-3333-333333333381'::uuid, '33333333-3333-3333-3333-333333333338'::uuid, 1, 'dialogue:nap-ham-cu-chi', 'Hỏi Chrono', 'Hỏi nắp hầm được giấu thế nào.', 'Gõ, nói hoặc gọi đều ghi chương.', 'Nhắc ngụy trang.', 'dialogue', 'Hỏi Chrono', 0, 'Nắp hầm Củ Chi được ngụy trang thế nào?', NULL::int, '/minigames/soi-nap-1.svg', NULL::uuid),
    ('33333333-3333-3333-3333-333333333382'::uuid, '33333333-3333-3333-3333-333333333338'::uuid, 2, 'minigame:soi-nap-cu-chi', 'Soi nắp', 'Tìm cửa ngụy trang.', 'Ba mức.', 'Chạm vào hình.', 'minigame', 'Chơi', 0, NULL::text, NULL::int, '/minigames/soi-nap-1.svg', 'a1000001-0000-4000-8000-000000000012'::uuid),
    ('33333333-3333-3333-3333-333333333383'::uuid, '33333333-3333-3333-3333-333333333338'::uuid, 3, 'minigame:im-lang', 'Im lặng', 'Giữ im hoặc giữ ngón tay.', 'Không có dữ kiện lịch sử. Mic chỉ xin khi vào game.', 'Giữ đủ lâu.', 'minigame', 'Chơi', 0, NULL::text, NULL::int, '/minigames/soi-nap-2.svg', 'a1000001-0000-4000-8000-000000000014'::uuid),
    ('33333333-3333-3333-3333-333333333391'::uuid, '33333333-3333-3333-3333-333333333339'::uuid, 1, 'dialogue:ba-tang-long-dat', 'Hỏi Chrono', 'Hỏi mỗi tầng chịu được gì.', 'Gõ, nói hoặc gọi đều ghi chương.', 'Nhắc tác dụng, không hỏi số mét đang tranh cãi.', 'dialogue', 'Hỏi Chrono', 0, 'Ba tầng địa đạo Củ Chi chịu được những gì?', NULL::int, '/media/cu-chi/scenes/phong-hop-2026.jpg', NULL::uuid),
    ('33333333-3333-3333-3333-333333333392'::uuid, '33333333-3333-3333-3333-333333333339'::uuid, 2, 'minigame:xep-tang-cu-chi', 'Xếp tầng', 'Ghép tác dụng với tầng.', 'Ba mức.', 'Kéo thẻ vào tầng.', 'minigame', 'Chơi', 0, NULL::text, NULL::int, '/media/cu-chi/scenes/phong-hop-2026.jpg', 'a1000001-0000-4000-8000-000000000013'::uuid)
) AS rows(id, quest_id, step_order, unlock_key, title, objective, description, hint, action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image, minigame_id)
WHERE EXISTS (SELECT 1 FROM quests WHERE id = '33333333-3333-3333-3333-333333333337')
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

INSERT INTO artifacts (
    id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order,
    collection_name, rarity, source_url, image_source, image_license, fact_verified, unlock_minigame_id
) VALUES
('c1000001-0000-4000-8000-000000000001', '11111111-1111-1111-1111-111111111111', 'Nồi trên bếp', '/artifacts/cu-chi/cc-01.svg', 'Khói từ trong lò bốc lên, qua các đường rãnh, thành làn khói mỏng bay là là trên mặt đất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-noi', 'tham khao', 201, 'Sinh hoạt', 'common', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000002', '11111111-1111-1111-1111-111111111111', 'Rãnh khói', '/artifacts/cu-chi/cc-02.svg', 'Bếp có nhiều đường rãnh để khói đi ra thành làn mỏng trên mặt đất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-ranh-khoi', 'tham khao', 202, 'Sinh hoạt', 'common', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000003', '11111111-1111-1111-1111-111111111111', 'Hầm khói', '/artifacts/cu-chi/cc-03.svg', 'Hệ thống thoát khói gồm hầm khói, đường dẫn khói và tia tản khói.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-ham-khoi', 'tham khao', 203, 'Sinh hoạt', 'common', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000004', '11111111-1111-1111-1111-111111111111', 'Tia tản khói', '/artifacts/cu-chi/cc-04.svg', 'Mỗi bếp thường có các tia tản khói để khói không bốc thành cột.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-tia-tan', 'tham khao', 204, 'Sinh hoạt', 'common', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000005', '11111111-1111-1111-1111-111111111111', 'Mô hình rãnh', '/artifacts/cu-chi/cc-05.svg', 'Khói đi qua đường rãnh dài rồi tản thành làn mỏng trên mặt đất.', 'Mở khi Đường khói đạt 3 sao.', 'artifact:cc-mo-hinh-ranh', 'tham khao', 205, 'Sinh hoạt', 'rare', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, 'a1000001-0000-4000-8000-000000000011'),
('c1000001-0000-4000-8000-000000000006', '11111111-1111-1111-1111-111111111111', 'Bếp trong địa đạo', '/artifacts/cu-chi/cc-06.svg', 'Khói từ lò đi qua rãnh và còn làn mỏng trên mặt đất.', 'Hiện khi đã qua cổng Premium 1948 hoặc 1968.', 'portal:cc-bep', 'tham khao', 206, 'Sinh hoạt', 'epic', 'https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000007', '11111111-1111-1111-1111-111111111111', 'Lá phủ miệng hầm', '/artifacts/cu-chi/cc-07.svg', 'Miệng hầm được ngụy trang để lẫn với mặt đất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-la-phu', 'tham khao', 207, 'Ngụy trang', 'common', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000008', '11111111-1111-1111-1111-111111111111', 'Nắp gỗ', '/artifacts/cu-chi/cc-08.svg', 'Cửa hầm có nắp được ngụy trang.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-nap-go', 'tham khao', 208, 'Ngụy trang', 'common', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000009', '11111111-1111-1111-1111-111111111111', 'Đất phủ nắp', '/artifacts/cu-chi/cc-09.svg', 'Lớp phủ giúp miệng hầm không lộ trên mặt đất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-dat-phu', 'tham khao', 209, 'Ngụy trang', 'common', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000010', '11111111-1111-1111-1111-111111111111', 'Cành ngụy trang', '/artifacts/cu-chi/cc-10.svg', 'Vật che được đặt để miệng hầm lẫn vào mặt đất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-canh', 'tham khao', 210, 'Ngụy trang', 'common', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000011', '11111111-1111-1111-1111-111111111111', 'Nắp đã soi', '/artifacts/cu-chi/cc-11.svg', 'Tìm đúng nắp ngụy trang trong ba mức Soi nắp.', 'Mở khi Soi nắp đạt 3 sao.', 'artifact:cc-nap-soi', 'tham khao', 211, 'Ngụy trang', 'rare', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, 'a1000001-0000-4000-8000-000000000012'),
('c1000001-0000-4000-8000-000000000012', '11111111-1111-1111-1111-111111111111', 'Cửa hầm năm 1948', '/artifacts/cu-chi/cc-12.svg', 'Nắp hầm thuộc lớp ngụy trang của địa đạo.', 'Hiện khi đã qua cổng Premium 1948 hoặc 1968.', 'portal:cc-cua-ham', 'tham khao', 212, 'Ngụy trang', 'epic', 'https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000013', '11111111-1111-1111-1111-111111111111', 'Tầng một', '/artifacts/cu-chi/cc-13.svg', 'Tầng 1 chịu được đạn pháo và sức nặng của xe tăng.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-tang-1', 'tham khao', 213, 'Hầm', 'common', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000014', '11111111-1111-1111-1111-111111111111', 'Tầng hai', '/artifacts/cu-chi/cc-14.svg', 'Tầng 2 chống được bom cỡ nhỏ.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-tang-2', 'tham khao', 214, 'Hầm', 'common', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000015', '11111111-1111-1111-1111-111111111111', 'Tầng ba', '/artifacts/cu-chi/cc-15.svg', 'Tầng 3 chống được hầu hết các loại bom đạn.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-tang-3', 'tham khao', 215, 'Hầm', 'common', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000016', '11111111-1111-1111-1111-111111111111', 'Lối tầng', '/artifacts/cu-chi/cc-16.svg', 'Địa đạo chia tầng theo việc chịu đạn và bom, không theo một số mét duy nhất.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-loi-tang', 'tham khao', 216, 'Hầm', 'common', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000017', '11111111-1111-1111-1111-111111111111', 'Sa bàn ba tầng', '/artifacts/cu-chi/cc-17.svg', 'Ghép đúng tác dụng của cả ba tầng.', 'Mở khi Xếp tầng đạt 3 sao.', 'artifact:cc-sa-ban-tang', 'tham khao', 217, 'Hầm', 'rare', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, 'a1000001-0000-4000-8000-000000000013'),
('c1000001-0000-4000-8000-000000000018', '11111111-1111-1111-1111-111111111111', 'Hầm qua cổng', '/artifacts/cu-chi/cc-18.svg', 'Ba tầng chịu đạn pháo, bom cỡ nhỏ và hầu hết bom đạn.', 'Hiện khi đã qua cổng Premium 1948 hoặc 1968.', 'portal:cc-ham', 'tham khao', 218, 'Hầm', 'epic', 'https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000019', '11111111-1111-1111-1111-111111111111', 'Miệng giếng', '/artifacts/cu-chi/cc-19.svg', 'Hai giếng cách nhau khoảng 6 đến 15 mét.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-mieng-gieng', 'tham khao', 219, 'Con người', 'common', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000020', '11111111-1111-1111-1111-111111111111', 'Độ sâu giếng', '/artifacts/cu-chi/cc-20.svg', 'Giếng sâu khoảng 3 đến 4 mét.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-do-sau-gieng', 'tham khao', 220, 'Con người', 'common', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000021', '11111111-1111-1111-1111-111111111111', 'Hướng đào', '/artifacts/cu-chi/cc-21.svg', 'Hai giếng được đào hướng vào nhau.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-huong-dao', 'tham khao', 221, 'Con người', 'common', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000022', '11111111-1111-1111-1111-111111111111', 'Khoảng cách hai giếng', '/artifacts/cu-chi/cc-22.svg', 'Khoảng cách giữa hai giếng khoảng 6 đến 15 mét, không phải độ sâu tầng.', 'Câu này chưa được người duyệt đánh dấu đã đọc nguồn.', 'artifact:cc-khoang-cach', 'tham khao', 222, 'Con người', 'common', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL),
('c1000001-0000-4000-8000-000000000023', '11111111-1111-1111-1111-111111111111', 'Phiên gác im', '/artifacts/cu-chi/cc-23.svg', 'Giữ im trong game không gắn thêm dữ kiện lịch sử.', 'Mở khi Im lặng đạt 3 sao.', 'artifact:cc-phien-gac', 'tham khao', 223, 'Con người', 'rare', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, 'a1000001-0000-4000-8000-000000000014'),
('c1000001-0000-4000-8000-000000000024', '11111111-1111-1111-1111-111111111111', 'Đội đào giếng', '/artifacts/cu-chi/cc-24.svg', 'Hai giếng cách khoảng 6 đến 15 mét và sâu khoảng 3 đến 4 mét.', 'Hiện khi đã qua cổng Premium 1948 hoặc 1968.', 'portal:cc-doi-dao', 'tham khao', 224, 'Con người', 'epic', 'https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html', 'Minh họa HistAR', 'Minh họa dự án, không phải ảnh báo hay phim', false, NULL)
ON CONFLICT (id) DO NOTHING;

UPDATE quests SET
    title = 'Bước chân ra thế giới',
    description = 'Chiêm ngưỡng rồng trên mái, Time Portal bến sông, vali mây.',
    story = 'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Nhìn rồng trên mái, so sánh ảnh xưa–nay, rồi khám phá vali mây.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
    title = 'Ngọn tháp bên sông Hương',
    description = 'Tháp Phước Duyên, Time Portal sông Hương, bia đá trên lưng rùa.',
    story = 'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chiêm ngưỡng tháp, xem Time Portal, rồi mở bia đá.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
    title = 'Dấu ấn kinh đô đầu tiên',
    description = 'Long Sàng đá, Time Portal kinh đô, đền Lê Đại Hành.',
    story = '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Khám phá Long Sàng, so sánh ảnh xưa–nay, rồi mở đền Lê Đại Hành.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
    title = 'Dấu ấn Hoàng Thành',
    description = 'Móng cột cổ, Time Portal Thăng Long, ngói men xanh.',
    story = 'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp hiện vật và Time Portal.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
    title = 'Bí ẩn Chùa Cầu',
    description = 'Chùa Cầu, Time Portal phố cổ, nhà Tấn Ký.',
    story = 'Hội An không chỉ đẹp trong ảnh. Nhìn Chùa Cầu, so sánh phố cổ, rồi mở nhà Tấn Ký.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
    title = 'Cổ thành đá Tây Đô',
    description = 'Cổng Nam, Time Portal Tây Đô, gạch in chữ.',
    story = 'Thành không vữa nhưng đứng vững 600 năm. Khám phá cổng Nam, Time Portal, rồi gạch in chữ Tây Đô.'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
    title = 'Đường đến khoa bảng',
    description = 'Khuê Văn Các, Time Portal Văn Miếu, Hồ Thiên Quang.',
    story = 'Chiêm ngưỡng Khuê Văn Các, xem Time Portal, rồi mở Hồ Thiên Quang.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
    title = 'Bí ẩn Đại Nội',
    description = 'Ngọ Môn, Time Portal Đại Nội, Cửu Đỉnh.',
    story = 'Chiêm ngưỡng Ngọ Môn, so sánh kinh thành, rồi mở Cửu Đỉnh.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
    title = 'Về Đất Tổ',
    description = 'Trống đồng, Time Portal Nghĩa Lĩnh, cột đá thế.',
    story = 'Chiêm ngưỡng trống đồng, xem Time Portal, rồi mở cột đá thế.'
WHERE id = '44444444-4444-4444-4444-444444444409';

-- END migration: V44__phase2_quests_collections.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V45__silence_relic_wording.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V45__silence_relic_wording.sql
-- ============================================================================
-- Món mở bằng Im lặng giữ câu của nguồn giếng, không gắn dữ kiện vào luật im lặng.

UPDATE artifacts
SET description = 'Hai giếng cách nhau khoảng 6 đến 15 mét và được đào hướng vào nhau.',
    story = 'Mở khi Im lặng đạt 3 sao. Câu này chưa được người duyệt đánh dấu đã đọc nguồn.'
WHERE id = 'c1000001-0000-4000-8000-000000000023';

-- END migration: V45__silence_relic_wording.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V46__journey_memory.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V46__journey_memory.sql
-- ============================================================================
CREATE TABLE journey_memories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_visited_poi VARCHAR(255),
    journey_summary VARCHAR(1000),
    locale VARCHAR(16),
    preferred_mode VARCHAR(32),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_journey_memories_user_location UNIQUE (user_id, location_id)
);

CREATE INDEX idx_journey_memories_user_expires ON journey_memories (user_id, expires_at);

-- END migration: V46__journey_memory.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V47__teacher_ai_drafts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V47__teacher_ai_drafts.sql
-- ============================================================================
CREATE TABLE teacher_ai_drafts (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_version VARCHAR(255) NOT NULL,
    answer_blocks_json TEXT NOT NULL,
    approved_locales_json TEXT NOT NULL,
    evaluation_passed BOOLEAN NOT NULL,
    evaluation_reasons TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    published_assignment_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_teacher_ai_drafts_teacher_created ON teacher_ai_drafts (teacher_id, created_at DESC);
CREATE INDEX idx_teacher_ai_drafts_org_status ON teacher_ai_drafts (org_id, status);

-- END migration: V47__teacher_ai_drafts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V48__narrative_investigation_foundation.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V48__narrative_investigation_foundation.sql
-- ============================================================================
CREATE TABLE narrative_investigations (
    id UUID PRIMARY KEY,
    quest_id UUID NOT NULL UNIQUE REFERENCES quests(id),
    version INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    graph_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE user_narrative_investigation_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    state_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_narrative_progress_user_investigation UNIQUE (user_id, investigation_id)
);

CREATE TABLE quest_reward_ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id),
    quest_id UUID NOT NULL REFERENCES quests(id),
    reward_type VARCHAR(48) NOT NULL,
    reward_key VARCHAR(255) NOT NULL,
    xp INTEGER NOT NULL,
    awarded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_quest_reward_ledger_idempotency UNIQUE (user_id, quest_id, reward_type, reward_key)
);

-- END migration: V48__narrative_investigation_foundation.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V49__quest_campaigns.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V49__quest_campaigns.sql
-- ============================================================================
CREATE TABLE quest_campaigns (
    id UUID PRIMARY KEY,
    quest_id UUID NOT NULL,
    campaign_type VARCHAR(24) NOT NULL,
    title VARCHAR(160) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_quest_campaigns_quest FOREIGN KEY (quest_id) REFERENCES quests(id),
    CONSTRAINT ck_quest_campaigns_window CHECK (ends_at > starts_at)
);
CREATE INDEX idx_quest_campaigns_active ON quest_campaigns (enabled, starts_at, ends_at);

-- END migration: V49__quest_campaigns.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V50__narrative_pilot_progress_and_review.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V50__narrative_pilot_progress_and_review.sql
-- ============================================================================
ALTER TABLE user_narrative_investigation_progress ADD COLUMN investigation_version INTEGER;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN visited_node_codes_json TEXT;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN evidence_codes_json TEXT;
ALTER TABLE user_narrative_investigation_progress ADD COLUMN selected_outcome_code VARCHAR(120);
ALTER TABLE narrative_investigations ADD COLUMN review_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT';
ALTER TABLE narrative_investigations ADD COLUMN approved_locales_json TEXT NOT NULL DEFAULT '[]';
ALTER TABLE narrative_investigations ADD COLUMN reviewed_by VARCHAR(160);
ALTER TABLE narrative_investigations ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
-- The Củ Chi pilot remains disabled until specialist review supplies approved citations/translations.
INSERT INTO narrative_investigations (id, quest_id, version, enabled, graph_json, created_at, updated_at, review_status, approved_locales_json)
SELECT '33333333-3333-3333-3333-333333333390'::uuid, '33333333-3333-3333-3333-333333333338'::uuid, 1, false,
'{"startNodeCode":"tour-360","nodes":[{"code":"tour-360","type":"TOUR_360_OBSERVATION","choices":[{"code":"interview","targetNodeCode":"ai-interview"}]},{"code":"ai-interview","type":"AI_INTERVIEW","choices":[{"code":"compare","targetNodeCode":"source-comparison"}]},{"code":"source-comparison","type":"SOURCE_COMPARISON","choices":[{"code":"evidence","targetNodeCode":"evidence-lid"}]},{"code":"evidence-lid","type":"EVIDENCE","choices":[{"code":"quick","targetNodeCode":"outcome-partial"},{"code":"full","targetNodeCode":"outcome-verified"}]},{"code":"outcome-partial","type":"OUTCOME","minimumEvidence":0,"choices":[]},{"code":"outcome-verified","type":"OUTCOME","minimumEvidence":1,"choices":[]}],"evidenceNodeCodes":["evidence-lid"],"outcomeNodeCodes":["outcome-partial","outcome-verified"]}', NOW(), NOW(), 'DRAFT', '[]'
WHERE NOT EXISTS (SELECT 1 FROM narrative_investigations WHERE quest_id='33333333-3333-3333-3333-333333333338'::uuid);

-- END migration: V50__narrative_pilot_progress_and_review.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V51__narrative_specialist_review_audit.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V51__narrative_specialist_review_audit.sql
-- ============================================================================
CREATE TABLE narrative_investigation_reviews (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    decision VARCHAR(24) NOT NULL,
    approved_locales_json TEXT NOT NULL,
    review_note TEXT,
    reviewed_by VARCHAR(160) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_narrative_investigation_reviews_investigation_time
    ON narrative_investigation_reviews (investigation_id, reviewed_at DESC);

-- END migration: V51__narrative_specialist_review_audit.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V52__cu_chi_nap_ham_review_ready_graph.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V52__cu_chi_nap_ham_review_ready_graph.sql
-- ============================================================================
-- Review-ready only. A specialist must approve all four locales through the admin review API
-- before this disabled investigation may be enabled for a pilot cohort.
UPDATE narrative_investigations
SET version = 2,
    enabled = FALSE,
    review_status = 'DRAFT',
    approved_locales_json = '[]',
    reviewed_by = NULL,
    reviewed_at = NULL,
    graph_json = '{"startNodeCode":"tour-360","sources":[{"sourceId":"rag-cu-chi-tong-quan","version":"2026-10-09","locale":"vi","citation":"Tổng quan Địa đạo Củ Chi (nguồn đã duyệt nội bộ)","path":"data/rag-approved/cu-chi-tong-quan.md"},{"sourceId":"minigame-nap-ham","version":"V43","locale":"vi","citation":"Soi nắp hầm — tư liệu tham chiếu DSVH","url":"https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"}],"nodes":[{"code":"tour-360","type":"TOUR_360_OBSERVATION","title":"Dấu hiệu trên mặt đất","prompt":"Trong Tour 360°, hãy quan sát nắp hầm và các dấu hiệu ngụy trang. Không suy luận vượt quá những gì thấy được.","actionHref":"/tour-360","choices":[{"code":"phỏng vấn nhân vật","targetNodeCode":"ai-interview"}]},{"code":"ai-interview","type":"AI_INTERVIEW","title":"Hỏi nhân vật AI","prompt":"Hỏi về cách kiểm chứng nắp hầm; AI chỉ được dựa trên hai nguồn được liệt kê.","choices":[{"code":"đối chiếu nguồn","targetNodeCode":"source-comparison"}]},{"code":"source-comparison","type":"SOURCE_COMPARISON","title":"Đối chiếu tư liệu","prompt":"So sánh quan sát Tour 360° với tư liệu đã duyệt; đánh dấu điểm nào là quan sát và điểm nào là diễn giải.","choices":[{"code":"ghi chứng cứ","targetNodeCode":"evidence-lid"}]},{"code":"evidence-lid","type":"EVIDENCE","title":"Mảnh chứng cứ: nắp hầm","prompt":"Chỉ ghi nhận sau khi đã hoàn thành quan sát và đối chiếu nguồn.","choices":[{"code":"kết luận nhanh","targetNodeCode":"outcome-partial"},{"code":"kết luận có kiểm chứng","targetNodeCode":"outcome-verified"}]},{"code":"outcome-partial","type":"OUTCOME","title":"Kết luận thăm dò","minimumEvidence":0,"prompt":"Đây là kết luận tạm thời, chưa xác minh đầy đủ.","choices":[]},{"code":"outcome-verified","type":"OUTCOME","title":"Kết luận có kiểm chứng","minimumEvidence":1,"prompt":"Kết luận dựa trên chứng cứ đã thu thập và nguồn đã duyệt.","choices":[]}],"evidenceNodeCodes":["evidence-lid"],"outcomeNodeCodes":["outcome-partial","outcome-verified"]}',
    updated_at = NOW()
WHERE quest_id = '33333333-3333-3333-3333-333333333338'::uuid;

-- END migration: V52__cu_chi_nap_ham_review_ready_graph.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V53__quest_campaign_audit.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V53__quest_campaign_audit.sql
-- ============================================================================
CREATE TABLE quest_campaign_audit (
  id UUID PRIMARY KEY, campaign_id UUID NOT NULL REFERENCES quest_campaigns(id), action VARCHAR(32) NOT NULL,
  actor_id UUID NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL, detail_json TEXT NOT NULL
);
CREATE INDEX idx_quest_campaign_audit_campaign_time ON quest_campaign_audit(campaign_id, occurred_at DESC);

-- END migration: V53__quest_campaign_audit.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V54__assignment_campaign_link.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V54__assignment_campaign_link.sql
-- ============================================================================
ALTER TABLE assignments ADD COLUMN campaign_id UUID REFERENCES quest_campaigns(id);
CREATE INDEX idx_assignments_campaign ON assignments(campaign_id);

-- END migration: V54__assignment_campaign_link.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V55__campaign_release_gate_and_rollback_reason.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V55__campaign_release_gate_and_rollback_reason.sql
-- ============================================================================
ALTER TABLE quest_campaigns ADD COLUMN pilot_status VARCHAR(24) NOT NULL DEFAULT 'PENDING';
ALTER TABLE quest_campaigns ADD COLUMN release_decision VARCHAR(24) NOT NULL DEFAULT 'PENDING';
ALTER TABLE quest_campaigns ADD COLUMN release_note TEXT;
ALTER TABLE quest_campaigns ADD COLUMN release_decided_by UUID;
ALTER TABLE quest_campaigns ADD COLUMN release_decided_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quest_campaign_audit ADD COLUMN rollback_reason TEXT;

-- END migration: V55__campaign_release_gate_and_rollback_reason.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V56__teacher_ai_draft_campaign_link.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V56__teacher_ai_draft_campaign_link.sql
-- ============================================================================
ALTER TABLE teacher_ai_drafts ADD COLUMN campaign_id UUID REFERENCES quest_campaigns(id);
CREATE INDEX idx_teacher_ai_drafts_campaign ON teacher_ai_drafts(campaign_id);

-- END migration: V56__teacher_ai_draft_campaign_link.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V57__cooperative_leaderboard_access_audit.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V57__cooperative_leaderboard_access_audit.sql
-- ============================================================================
CREATE TABLE cooperative_leaderboard_access_audit (
    id UUID PRIMARY KEY,
    actor_id UUID,
    organization_id UUID NOT NULL,
    campaign_id UUID NOT NULL,
    allowed BOOLEAN NOT NULL,
    outcome VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail TEXT
);
CREATE INDEX idx_coop_lb_access_audit_time ON cooperative_leaderboard_access_audit(organization_id, occurred_at DESC);

-- END migration: V57__cooperative_leaderboard_access_audit.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V58__repair_pgvector_rag_schema.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V58__repair_pgvector_rag_schema.sql
-- ============================================================================
-- Repair legacy databases where V25 was recorded after rag_chunks / semantic_cache
-- already existed without their vector columns. Do not fail non-pgvector hosts:
-- RAG remains disabled there until the host is upgraded to pgvector.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_available_extensions WHERE name = 'vector') THEN
        CREATE EXTENSION IF NOT EXISTS vector;

        IF to_regclass('public.rag_chunks') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE public.rag_chunks ADD COLUMN IF NOT EXISTS embedding vector(768)';
        END IF;

        IF to_regclass('public.semantic_cache') IS NOT NULL THEN
            EXECUTE 'ALTER TABLE public.semantic_cache ADD COLUMN IF NOT EXISTS embedding vector(768)';
        END IF;
    ELSE
        RAISE NOTICE 'pgvector is not available on this PostgreSQL host; RAG embeddings remain disabled.';
    END IF;
END $$;

-- END migration: V58__repair_pgvector_rag_schema.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V59__repair_phase2_schema_constraints.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V59__repair_phase2_schema_constraints.sql
-- ============================================================================
-- Reconcile databases that received Phase 2/3 tables from Hibernate before
-- Flyway V46-V57 was recorded. The earlier migrations are now idempotent;
-- this migration restores the constraints and indexes they require.

CREATE INDEX IF NOT EXISTS idx_journey_memories_user_expires ON journey_memories (user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_teacher_created ON teacher_ai_drafts (teacher_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_org_status ON teacher_ai_drafts (org_id, status);
CREATE INDEX IF NOT EXISTS idx_quest_campaigns_active ON quest_campaigns (enabled, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_narrative_investigation_reviews_investigation_time
    ON narrative_investigation_reviews (investigation_id, reviewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_quest_campaign_audit_campaign_time
    ON quest_campaign_audit (campaign_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignments_campaign ON assignments(campaign_id);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_campaign ON teacher_ai_drafts(campaign_id);
CREATE INDEX IF NOT EXISTS idx_coop_lb_access_audit_time
    ON cooperative_leaderboard_access_audit(organization_id, occurred_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_investigations_quest') THEN
        ALTER TABLE narrative_investigations ADD CONSTRAINT fk_narrative_investigations_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_progress_user') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT fk_narrative_progress_user FOREIGN KEY (user_id) REFERENCES profiles(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_progress_investigation') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT fk_narrative_progress_investigation FOREIGN KEY (investigation_id) REFERENCES narrative_investigations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_narrative_progress_user_investigation') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT uq_narrative_progress_user_investigation UNIQUE (user_id, investigation_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_reward_ledger_user') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT fk_quest_reward_ledger_user FOREIGN KEY (user_id) REFERENCES profiles(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_reward_ledger_quest') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT fk_quest_reward_ledger_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_quest_reward_ledger_idempotency') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT uq_quest_reward_ledger_idempotency UNIQUE (user_id, quest_id, reward_type, reward_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_campaigns_quest') THEN
        ALTER TABLE quest_campaigns ADD CONSTRAINT fk_quest_campaigns_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quest_campaigns_window') THEN
        ALTER TABLE quest_campaigns ADD CONSTRAINT ck_quest_campaigns_window CHECK (ends_at > starts_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_investigation_reviews_investigation') THEN
        ALTER TABLE narrative_investigation_reviews ADD CONSTRAINT fk_narrative_investigation_reviews_investigation FOREIGN KEY (investigation_id) REFERENCES narrative_investigations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_campaign_audit_campaign') THEN
        ALTER TABLE quest_campaign_audit ADD CONSTRAINT fk_quest_campaign_audit_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assignments_campaign') THEN
        ALTER TABLE assignments ADD CONSTRAINT fk_assignments_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_teacher_ai_drafts_campaign') THEN
        ALTER TABLE teacher_ai_drafts ADD CONSTRAINT fk_teacher_ai_drafts_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
END $$;

-- END migration: V59__repair_phase2_schema_constraints.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V60__cu_chi_vietnamese_only_pilot_citations.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V60__cu_chi_vietnamese_only_pilot_citations.sql
-- ============================================================================
-- The Củ Chi pilot is Vietnamese-only.  This migration deliberately keeps the
-- pilot disabled and in DRAFT until an authorised reviewer records approval.
UPDATE narrative_investigations
SET version = 3,
    enabled = FALSE,
    review_status = 'DRAFT',
    approved_locales_json = '[]',
    reviewed_by = NULL,
    reviewed_at = NULL,
    graph_json = $graph$
{
  "startNodeCode": "tour-360",
  "sources": [
    {
      "sourceId": "dsvh-cu-chi-history-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, đoạn về hình thành khoảng năm 1948 và việc gia cố, mở rộng trong kháng chiến chống Mỹ.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-ben-dinh-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, danh mục thành phần Địa đạo Bến Đình có hầm bí mật (nắp hầm).",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-status-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, ‘Di tích lịch sử Địa đạo Củ Chi’, thông tin xếp hạng di tích quốc gia đặc biệt theo Quyết định 2367/QĐ-TTg ngày 23-12-2015.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    }
  ],
  "nodes": [
    {
      "code": "tour-360",
      "type": "TOUR_360_OBSERVATION",
      "title": "Dấu hiệu trên mặt đất",
      "prompt": "Trong Tour 360°, quan sát nắp hầm và chỉ ghi nhận chi tiết bạn thực sự nhìn thấy. Không kết luận về vật liệu, cách ngụy trang hoặc cơ chế hoạt động nếu tư liệu không xác nhận.",
      "actionHref": "/tour-360",
      "choices": [{"code": "đặt câu hỏi kiểm chứng", "targetNodeCode": "ai-interview"}]
    },
    {
      "code": "ai-interview",
      "type": "AI_INTERVIEW",
      "title": "Hỏi nhân vật AI",
      "prompt": "Hỏi về bối cảnh lịch sử của nắp hầm. AI chỉ được sử dụng các nguồn đã liệt kê, phân biệt rõ fact đã xác minh với diễn giải.",
      "choices": [{"code": "đối chiếu tư liệu", "targetNodeCode": "source-comparison"}]
    },
    {
      "code": "source-comparison",
      "type": "SOURCE_COMPARISON",
      "title": "Đối chiếu tư liệu",
      "prompt": "So sánh quan sát Tour 360° với tư liệu Cục Di sản văn hóa; đánh dấu nội dung nào là quan sát và nội dung nào là fact có citation.",
      "choices": [{"code": "ghi chứng cứ", "targetNodeCode": "evidence-lid"}]
    },
    {
      "code": "evidence-lid",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: nắp hầm",
      "prompt": "Ghi nhận rằng tư liệu chính thức liệt kê hầm bí mật (nắp hầm) tại Bến Đình. Đây không phải hướng dẫn chế tạo, vận hành hay né tránh cạm bẫy.",
      "choices": [
        {"code": "kết luận thăm dò", "targetNodeCode": "outcome-partial"},
        {"code": "kết luận có kiểm chứng", "targetNodeCode": "outcome-verified"}
      ]
    },
    {
      "code": "outcome-partial",
      "type": "OUTCOME",
      "title": "Kết luận thăm dò",
      "minimumEvidence": 0,
      "prompt": "Bạn đã có một nhận định ban đầu; cần thêm chứng cứ trước khi xem đó là kết luận đã kiểm chứng.",
      "choices": []
    },
    {
      "code": "outcome-verified",
      "type": "OUTCOME",
      "title": "Kết luận có kiểm chứng",
      "minimumEvidence": 1,
      "prompt": "Kết luận dựa trên quan sát, chứng cứ đã thu thập và tư liệu chính thức đã trích dẫn.",
      "choices": []
    }
  ],
  "evidenceNodeCodes": ["evidence-lid"],
  "outcomeNodeCodes": ["outcome-partial", "outcome-verified"]
}
$graph$,
    updated_at = NOW()
WHERE quest_id = '33333333-3333-3333-3333-333333333338'::uuid;

-- END migration: V60__cu_chi_vietnamese_only_pilot_citations.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V61__admin_audit_events.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V61__admin_audit_events.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS admin_audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(96) NOT NULL,
    old_row_version BIGINT,
    new_row_version BIGINT,
    reason TEXT,
    idempotency_key VARCHAR(160),
    metadata_json TEXT NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_events_entity_occurred
    ON admin_audit_events(entity_type, entity_id, occurred_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_audit_events_idempotency
    ON admin_audit_events(entity_type, entity_id, action, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_discovery_points_deleted_at ON discovery_points(deleted_at);
CREATE INDEX IF NOT EXISTS idx_artifacts_deleted_at ON artifacts(deleted_at);
CREATE INDEX IF NOT EXISTS idx_quests_deleted_at ON quests(deleted_at);

-- END migration: V61__admin_audit_events.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V62__chrono_only_chat_agents.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V62__chrono_only_chat_agents.sql
-- ============================================================================
-- Keep historical characters and their conversations intact, but expose only
-- Chrono as the technical chat agent for new conversations.
ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS chat_enabled BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE characters
SET chat_enabled = FALSE
WHERE name <> 'Chrono — Trợ lý TimeLens'
   OR era IS DISTINCT FROM 'TimeLens';

CREATE UNIQUE INDEX IF NOT EXISTS ux_characters_enabled_chat_agent_per_location
    ON characters (location_id)
    WHERE chat_enabled;

INSERT INTO characters (location_id, name, era, persona_prompt, portrait_url, chat_enabled)
SELECT
    locations.id,
    'Chrono — Trợ lý TimeLens',
    'TimeLens',
    'Bạn là Chrono, Trợ lý TimeLens. Chỉ trả lời bằng thông tin có trong tư liệu và nguồn được cung cấp. Phân biệt rõ fact đã kiểm chứng với điều chưa đủ dữ liệu, không tự tạo nguồn, không hướng dẫn hành vi nguy hiểm, và không tiết lộ dữ liệu hay lịch sử của người dùng khác.',
    '/media/mascot/mascot-5.png',
    TRUE
FROM locations
ON CONFLICT (location_id) WHERE chat_enabled
DO UPDATE SET
    name = EXCLUDED.name,
    era = EXCLUDED.era,
    persona_prompt = EXCLUDED.persona_prompt,
    portrait_url = EXCLUDED.portrait_url,
    chat_enabled = TRUE;

-- Suggested prompts are part of the chat surface too.  Preserve the records,
-- but make the canonical Chrono key and wording available to every client.
DELETE FROM station_chat_prompts legacy
USING station_chat_prompts canonical
WHERE legacy.persona <> 'chrono'
  AND canonical.persona = 'chrono'
  AND canonical.site_code = legacy.site_code
  AND canonical.station_code = legacy.station_code
  AND canonical.sort_order = legacy.sort_order;

UPDATE station_chat_prompts
SET persona = 'chrono',
    question_text = replace(question_text, 'Chị Năm', 'Chrono')
WHERE persona <> 'chrono';

-- END migration: V62__chrono_only_chat_agents.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V63__verify_seed_accounts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V63__verify_seed_accounts.sql
-- ============================================================================
-- Seeded platform accounts are controlled demo/test identities. They must not
-- enter the end-user email-verification flow after a database reset or reseed.
UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn')
  AND email_verified IS DISTINCT FROM TRUE;

-- END migration: V63__verify_seed_accounts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V64__repair_admin_audit_schema.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V64__repair_admin_audit_schema.sql
-- ============================================================================
-- Repair databases that were baselined or bootstrapped before the complete
-- admin audit/optimistic-lock schema from V61 was applied.
-- Every statement is idempotent so this migration is also safe on healthy DBs.

CREATE TABLE IF NOT EXISTS admin_audit_events (
    id UUID PRIMARY KEY,
    actor_id UUID,
    entity_type VARCHAR(64) NOT NULL,
    entity_id UUID NOT NULL,
    action VARCHAR(96) NOT NULL,
    old_row_version BIGINT,
    new_row_version BIGINT,
    reason TEXT,
    idempotency_key VARCHAR(160),
    metadata_json TEXT NOT NULL DEFAULT '{}',
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_admin_audit_events_entity_occurred
    ON admin_audit_events(entity_type, entity_id, occurred_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_admin_audit_events_idempotency
    ON admin_audit_events(entity_type, entity_id, action, idempotency_key)
    WHERE idempotency_key IS NOT NULL;

ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE discovery_points ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS fact_verified BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS deleted_by UUID;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS delete_reason TEXT;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS row_version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_discovery_points_deleted_at ON discovery_points(deleted_at);
CREATE INDEX IF NOT EXISTS idx_artifacts_deleted_at ON artifacts(deleted_at);
CREATE INDEX IF NOT EXISTS idx_quests_deleted_at ON quests(deleted_at);

-- END migration: V64__repair_admin_audit_schema.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V65__cu_chi_narrative_flow_v4.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V65__cu_chi_narrative_flow_v4.sql
-- ============================================================================
-- Rich Vietnamese-only narrative flow. It remains disabled/DRAFT until reviewed in Admin Console.
UPDATE narrative_investigations
SET version = 4,
    enabled = FALSE,
    review_status = 'DRAFT',
    approved_locales_json = '[]',
    reviewed_by = NULL,
    reviewed_at = NULL,
    updated_at = NOW(),
    graph_json = $graph$
{
  "startNodeCode": "tour-360",
  "sources": [
    {
      "sourceId": "dsvh-cu-chi-history-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, Di tích lịch sử Địa đạo Củ Chi: thông tin hình thành, cấu trúc và giá trị di tích.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    },
    {
      "sourceId": "dsvh-cu-chi-ben-dinh-2026-10-12",
      "version": "accessed-2026-10-12",
      "locale": "vi",
      "citation": "Cục Di sản văn hóa, Di tích lịch sử Địa đạo Củ Chi: hạng mục hầm bí mật/nắp hầm tại Bến Đình.",
      "url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490"
    }
  ],
  "nodes": [
    {
      "code": "tour-360",
      "type": "TOUR_360_OBSERVATION",
      "title": "Truy tìm dấu hiệu trên mặt đất",
      "prompt": "Bạn là điều tra viên hiện trường. Mở Tour 360° và ghi lại đúng những gì mắt thấy quanh miệng hầm. Không đoán vật liệu hay cơ chế nếu chưa có nguồn xác nhận.",
      "actionHref": "/tour-360",
      "factLabel": "NHIỆM VỤ QUAN SÁT",
      "observationChecklist": ["Xác định vị trí miệng/nắp hầm trong khung hình", "Ghi một chi tiết nhìn thấy rõ", "Phân biệt quan sát trực tiếp với suy đoán"],
      "choices": [{"code": "Tôi đã ghi nhận dấu hiệu", "targetNodeCode": "ai-interview"}]
    },
    {
      "code": "ai-interview",
      "type": "AI_INTERVIEW",
      "title": "Phỏng vấn Chrono",
      "prompt": "Đặt một câu hỏi kiểm chứng cho Chrono về bối cảnh của nắp hầm. Câu trả lời chỉ là giả thuyết dẫn đường; hãy mở nguồn để xác nhận.",
      "factLabel": "CÂU HỎI KIỂM CHỨNG",
      "observationChecklist": ["Hỏi một điều có thể đối chiếu bằng nguồn", "Đánh dấu phần Chrono nói là fact hay diễn giải", "Không yêu cầu hướng dẫn chế tạo hoặc né tránh cạm bẫy"],
      "choices": [{"code": "Mở hồ sơ nguồn", "targetNodeCode": "source-comparison"}]
    },
    {
      "code": "source-comparison",
      "type": "SOURCE_COMPARISON",
      "title": "Đối chiếu tư liệu chính thống",
      "prompt": "So sánh ghi chép hiện trường với nguồn Cục Di sản văn hóa. Chỉ đánh dấu chứng cứ khi nguồn nói rõ điều đó.",
      "factLabel": "FACT CÓ CITATION",
      "sourceIds": ["dsvh-cu-chi-history-2026-10-12", "dsvh-cu-chi-ben-dinh-2026-10-12"],
      "observationChecklist": ["Mở ít nhất một source", "Tìm câu nói về hầm bí mật/nắp hầm", "Ghi lại URL nguồn trong hồ sơ"],
      "choices": [{"code": "Ghi mảnh bối cảnh", "targetNodeCode": "evidence-context"}]
    },
    {
      "code": "evidence-context",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: bối cảnh địa đạo",
      "prompt": "Bạn đã đối chiếu được bối cảnh của hầm và nắp hầm với nguồn chính thống. Ghi mảnh này vào hồ sơ, rồi hoàn thành Soi nắp để kiểm chứng dấu hiệu quan sát.",
      "factLabel": "BỐI CẢNH ĐÃ ĐỐI CHIẾU",
      "sourceIds": ["dsvh-cu-chi-history-2026-10-12", "dsvh-cu-chi-ben-dinh-2026-10-12"],
      "evidenceKey": "tunnel-context",
      "rewardXp": 10,
      "choices": [{"code": "Mở thử thách Soi nắp", "targetNodeCode": "evidence-lid"}]
    },
    {
      "code": "evidence-lid",
      "type": "EVIDENCE",
      "title": "Mảnh chứng cứ: nắp hầm",
      "prompt": "Hoàn thành Soi nắp để chứng minh bạn đã quan sát đúng dấu hiệu. Sau đó quay lại đây để ghi nhận mảnh chứng cứ.",
      "factLabel": "ĐÃ ĐỐI CHIẾU",
      "sourceIds": ["dsvh-cu-chi-ben-dinh-2026-10-12"],
      "evidenceKey": "lid-observation",
      "linkedMinigameId": "a1000001-0000-4000-8000-000000000012",
      "rewardXp": 10,
      "choices": [{"code": "Kết luận thăm dò", "targetNodeCode": "outcome-partial"}, {"code": "Kết luận hồ sơ", "targetNodeCode": "outcome-verified"}]
    },
    {
      "code": "outcome-partial",
      "type": "OUTCOME",
      "title": "Kết luận thăm dò",
      "minimumEvidence": 0,
      "prompt": "Bạn đã hình thành giả thuyết ban đầu. Hãy quay lại Tour 360° hoặc mở nguồn để củng cố hồ sơ.",
      "factLabel": "CHƯA ĐỦ CHỨNG CỨ",
      "choices": []
    },
    {
      "code": "outcome-verified",
      "type": "OUTCOME",
      "title": "Kết luận có kiểm chứng",
      "minimumEvidence": 2,
      "prompt": "Bạn đã nối được quan sát hiện trường, bối cảnh tư liệu, mini-game và citation chính thống thành một kết luận có kiểm chứng.",
      "factLabel": "HỒ SƠ ĐÃ XÁC MINH",
      "rewardXp": 20,
      "choices": []
    }
  ],
  "evidenceNodeCodes": ["evidence-context", "evidence-lid"],
  "outcomeNodeCodes": ["outcome-partial", "outcome-verified"]
}
$graph$
WHERE quest_id = '33333333-3333-3333-3333-333333333338'::uuid;

-- END migration: V65__cu_chi_narrative_flow_v4.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V66__narrative_observation_progress.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V66__narrative_observation_progress.sql
-- ============================================================================
ALTER TABLE user_narrative_investigation_progress
    ADD COLUMN IF NOT EXISTS observation_codes_json TEXT NOT NULL DEFAULT '[]';

-- END migration: V66__narrative_observation_progress.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V67__narrative_chat_progress.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V67__narrative_chat_progress.sql
-- ============================================================================
ALTER TABLE user_narrative_investigation_progress
    ADD COLUMN IF NOT EXISTS chat_node_codes_json TEXT NOT NULL DEFAULT '[]';

-- END migration: V67__narrative_chat_progress.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V68__narrative_investigation_versioning.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V68__narrative_investigation_versioning.sql
-- ============================================================================
CREATE TABLE narrative_investigation_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    version INTEGER NOT NULL,
    graph_json TEXT NOT NULL,
    review_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    approved_locales_json TEXT NOT NULL DEFAULT '[]',
    reviewed_by VARCHAR(160),
    reviewed_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    open_draft BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_narrative_version_number UNIQUE (investigation_id, version),
    CONSTRAINT ck_narrative_version_review_status
        CHECK (review_status IN ('DRAFT', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_narrative_open_draft
    ON narrative_investigation_versions(investigation_id)
    WHERE open_draft;

ALTER TABLE narrative_investigations
    ADD COLUMN published_version_id UUID,
    ADD COLUMN rollout_state VARCHAR(24) NOT NULL DEFAULT 'NOT_RELEASED',
    ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE narrative_investigations
    ADD CONSTRAINT ck_narrative_rollout_state
        CHECK (rollout_state IN ('NOT_RELEASED', 'ACTIVE', 'PAUSED'));

INSERT INTO narrative_investigation_versions (
    id, investigation_id, version, graph_json, review_status, approved_locales_json,
    reviewed_by, reviewed_at, revision, open_draft, created_at, updated_at)
SELECT gen_random_uuid(), ni.id, ni.version, ni.graph_json, ni.review_status,
       ni.approved_locales_json, ni.reviewed_by, ni.reviewed_at, 0,
       ni.review_status <> 'APPROVED', ni.created_at, ni.updated_at
FROM narrative_investigations ni;

UPDATE narrative_investigations ni
SET published_version_id = niv.id,
    rollout_state = CASE
        WHEN ni.enabled AND ni.review_status = 'APPROVED' THEN 'ACTIVE'
        WHEN NOT ni.enabled AND ni.review_status = 'APPROVED' THEN 'PAUSED'
        ELSE 'NOT_RELEASED'
    END
FROM narrative_investigation_versions niv
WHERE niv.investigation_id = ni.id
  AND niv.version = ni.version
  AND ni.review_status = 'APPROVED';

ALTER TABLE narrative_investigations
    ADD CONSTRAINT fk_narrative_published_version
        FOREIGN KEY (published_version_id) REFERENCES narrative_investigation_versions(id);

ALTER TABLE narrative_investigation_reviews
    ADD COLUMN version_id UUID,
    ADD COLUMN version INTEGER,
    ADD COLUMN revision BIGINT,
    ADD COLUMN audit_event_id UUID;

UPDATE narrative_investigation_reviews nir
SET version_id = niv.id,
    version = niv.version,
    revision = niv.revision
FROM narrative_investigation_versions niv
WHERE niv.investigation_id = nir.investigation_id;

ALTER TABLE narrative_investigation_reviews
    ADD CONSTRAINT fk_narrative_review_version
        FOREIGN KEY (version_id) REFERENCES narrative_investigation_versions(id),
    ADD CONSTRAINT fk_narrative_review_audit
        FOREIGN KEY (audit_event_id) REFERENCES admin_audit_events(id),
    ADD CONSTRAINT uq_narrative_review_audit UNIQUE (audit_event_id);

CREATE TABLE narrative_rollout_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    idempotency_key VARCHAR(160) NOT NULL,
    action VARCHAR(32) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_narrative_rollout_idempotency UNIQUE (investigation_id, idempotency_key)
);

CREATE INDEX idx_narrative_versions_investigation
    ON narrative_investigation_versions(investigation_id, version DESC);
CREATE INDEX idx_narrative_rollout_idempotency_created
    ON narrative_rollout_idempotency(investigation_id, created_at DESC);

-- END migration: V68__narrative_investigation_versioning.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V69__admin_console_phase_1_2.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V69__admin_console_phase_1_2.sql
-- ============================================================================
CREATE TABLE content_report_cases (
    event_id UUID PRIMARY KEY REFERENCES pilot_events(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    admin_note TEXT,
    reviewed_by UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_content_report_case_status
        CHECK (status IN ('NEW', 'IN_REVIEW', 'RESOLVED', 'REJECTED'))
);

INSERT INTO content_report_cases (event_id, status, created_at, updated_at)
SELECT id, 'NEW', COALESCE(created_at, NOW()), COALESCE(created_at, NOW())
FROM pilot_events
WHERE event_type = 'content_report'
ON CONFLICT (event_id) DO NOTHING;

CREATE INDEX idx_content_report_cases_status_updated
    ON content_report_cases (status, updated_at DESC);

ALTER TABLE org_payment_transactions
    ADD COLUMN license_count INTEGER,
    ADD COLUMN gross_amount_vnd BIGINT,
    ADD COLUMN discount_amount_vnd BIGINT,
    ADD COLUMN subscription_id UUID;

ALTER TABLE org_payment_transactions
    ADD CONSTRAINT ck_org_payment_license_count CHECK (license_count IS NULL OR license_count > 0),
    ADD CONSTRAINT ck_org_payment_gross_amount CHECK (gross_amount_vnd IS NULL OR gross_amount_vnd >= 0),
    ADD CONSTRAINT ck_org_payment_discount_amount CHECK (discount_amount_vnd IS NULL OR discount_amount_vnd >= 0),
    ADD CONSTRAINT fk_org_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id);

CREATE INDEX idx_org_payment_transactions_subscription
    ON org_payment_transactions (subscription_id) WHERE subscription_id IS NOT NULL;

ALTER TABLE subscriptions
    ADD COLUMN source_payment_transaction_id UUID,
    ADD COLUMN billing_cadence VARCHAR(24),
    ADD COLUMN contract_value_vnd BIGINT,
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN quantity INTEGER;

ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscription_source_payment
        FOREIGN KEY (source_payment_transaction_id) REFERENCES org_payment_transactions(id),
    ADD CONSTRAINT ck_subscription_billing_cadence
        CHECK (billing_cadence IS NULL OR billing_cadence IN ('MONTHLY', 'QUARTERLY', 'ANNUAL', 'ONE_TIME')),
    ADD CONSTRAINT ck_subscription_contract_value
        CHECK (contract_value_vnd IS NULL OR contract_value_vnd >= 0),
    ADD CONSTRAINT ck_subscription_currency
        CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_subscription_quantity CHECK (quantity IS NULL OR quantity > 0);

CREATE UNIQUE INDEX uq_subscriptions_source_payment
    ON subscriptions (source_payment_transaction_id)
    WHERE source_payment_transaction_id IS NOT NULL;

-- END migration: V69__admin_console_phase_1_2.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V70__organization_ops_note.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V70__organization_ops_note.sql
-- ============================================================================
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS ops_note TEXT;

UPDATE organizations
SET ops_note = 'Đối tác chiến lược: gói ưu tiên bổ sung tài khoản hướng dẫn viên khảo sát đợt tiếp theo.'
WHERE slug = 'cu-chi-demo'
  AND (ops_note IS NULL OR BTRIM(ops_note) = '');

UPDATE organizations
SET ops_note = 'Tổ chức demo STANDARD — dùng cho kiểm thử roster và analytics admin.'
WHERE slug = 'demo-standard-org'
  AND (ops_note IS NULL OR BTRIM(ops_note) = '');

-- END migration: V70__organization_ops_note.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V71__admin_billing_crm_pipeline_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V71__admin_billing_crm_pipeline_seed.sql
-- ============================================================================
-- Seed B2B commercial inquiries for Admin Billing & CRM Kanban (idempotent).
-- Statuses map to Stitch pipeline: NEW → CONTACTED → QUALIFIED → WON.

DELETE FROM heritage_digitization_inquiries
WHERE contact_email LIKE 'seed-crm-%@histar.vn';

INSERT INTO heritage_digitization_inquiries (
    id, site_name, contact_name, contact_email, contact_phone, package_type, message,
    status, admin_notes, contacted_at, interest_site_code, created_at, updated_at
) VALUES
(
    gen_random_uuid(),
    'Trường THCS Chu Văn An',
    'Thầy Nguyễn Văn Tùng',
    'seed-crm-lead-cva@histar.vn',
    '0912839001',
    'EDU_K12',
    'Đăng ký tư vấn gói trải nghiệm di sản cho học sinh khối 8-9 (~800 users).',
    'NEW',
    'Lead vào từ form B2B2C — ưu tiên gọi trong 48h.',
    NULL,
    'cu-chi',
    NOW() - INTERVAL '2 hours',
    NOW() - INTERVAL '2 hours'
),
(
    gen_random_uuid(),
    'Vietravel Chi nhánh Ba Đình',
    'Bà Trần Bích Hạnh',
    'seed-crm-lead-vietravel@histar.vn',
    '0904123456',
    'TRAVEL',
    'Cần tích hợp app AR định vị cho đoàn inbound châu Á (~1.500 users).',
    'NEW',
    'Lead vào từ hội thảo du lịch di sản.',
    NULL,
    'hoa-lo',
    NOW() - INTERVAL '5 hours',
    NOW() - INTERVAL '5 hours'
),
(
    gen_random_uuid(),
    'Bảo tàng Mỹ thuật Việt Nam',
    'Lê Hoàng Nam',
    'seed-crm-lead-finearts@histar.vn',
    '0988111222',
    'MUSEUM',
    'Quan tâm gói số hóa thuyết minh AR cho triển lãm tạm thời.',
    'NEW',
    NULL,
    NULL,
    'fine-arts',
    NOW() - INTERVAL '1 day',
    NOW() - INTERVAL '1 day'
),
(
    gen_random_uuid(),
    'Trường THPT Chuyên Hà Nội - Amsterdam',
    'Cô Phạm Thu Hà',
    'seed-crm-lead-amsterdam@histar.vn',
    '0919000111',
    'EDU_K12',
    'Pilot ngoại khóa lịch sử cho khối 10 (~600 users).',
    'NEW',
    'Đã gửi brochure PDF.',
    NULL,
    'thang-long',
    NOW() - INTERVAL '30 hours',
    NOW() - INTERVAL '30 hours'
),
(
    gen_random_uuid(),
    'Khu Di tích Nhà tù Hỏa Lò',
    'Nguyễn Quốc Huy',
    'seed-crm-pilot-hoalo@histar.vn',
    '0988123456',
    'MUSEUM',
    'Đang cấp tài khoản thử nghiệm 50 beacon Bluetooth tại phòng giam số 4.',
    'CONTACTED',
    'Đã test 3/7 ngày — theo dõi retention học sinh tham quan.',
    NOW() - INTERVAL '3 days',
    'hoa-lo',
    NOW() - INTERVAL '10 days',
    NOW() - INTERVAL '1 day'
),
(
    gen_random_uuid(),
    'Trường Đại học Sư phạm Hà Nội',
    'TS. Hoàng Minh Châu',
    'seed-crm-pilot-hnue@histar.vn',
    '0243754700',
    'EDU_UNI',
    'Pilot lớp phương pháp giảng dạy lịch sử với AR quest (~400 users).',
    'CONTACTED',
    'Đã bàn giao 20 tài khoản teacher demo.',
    NOW() - INTERVAL '5 days',
    'cu-chi',
    NOW() - INTERVAL '14 days',
    NOW() - INTERVAL '2 days'
),
(
    gen_random_uuid(),
    'Đại học Khoa học Xã hội & Nhân văn',
    'PGS.TS Lê Đức Toàn',
    'seed-crm-negotiate-ussh@histar.vn',
    '02438580000',
    'EDU_UNI',
    'Đã gửi báo giá 1.200 vé AR quest Hoàng thành. Chờ phòng Kế hoạch duyệt.',
    'QUALIFIED',
    'Giá trị dự kiến: 144.000.000 ₫ — chờ chốt deal.',
    NOW() - INTERVAL '12 days',
    'thang-long',
    NOW() - INTERVAL '28 days',
    NOW() - INTERVAL '6 hours'
),
(
    gen_random_uuid(),
    'Trung tâm Bảo tồn Di sản Thăng Long',
    'Giám đốc Ban Quản lý',
    'seed-crm-won-thanglong@histar.vn',
    '02437450815',
    'HERITAGE',
    'Hợp đồng HĐ-2024-B09 — đã thu cọc 50%, bàn giao 50 trạm AR.',
    'WON',
    'Triển khai: Active. Tài khoản admin trường đã cấp phát.',
    NOW() - INTERVAL '40 days',
    'thang-long',
    NOW() - INTERVAL '90 days',
    NOW() - INTERVAL '7 days'
),
(
    gen_random_uuid(),
    'Bảo tàng Lịch sử Quốc gia',
    'Trần Thị Lan',
    'seed-crm-won-history@histar.vn',
    '02438253841',
    'MUSEUM',
    'Gói thuyết minh đa ngôn ngữ cho khách quốc tế.',
    'WON',
    'Đã ký kết Q3/2026 — đang vận hành ổn định.',
    NOW() - INTERVAL '60 days',
    'history-museum',
    NOW() - INTERVAL '120 days',
    NOW() - INTERVAL '20 days'
);

-- END migration: V71__admin_billing_crm_pipeline_seed.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V72__admin_analytics_cu_chi_demo_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V72__admin_analytics_cu_chi_demo_seed.sql
-- ============================================================================
-- Admin Analytics demo seed for Äá»‹a Ä‘áº¡o Cá»§ Chi (location 11111111-...).
-- Proportions inspired by Stitch AnalyticsView mock, scaled for local DB (K-anonymity threshold = 10).
-- Idempotent: removes prior seed-analytics-* rows then re-inserts.

-- Applied inside Flyway transaction.

-- Session-scoped TEMP (no ON COMMIT DROP): bootstrap runs via psql autocommit;
-- ON COMMIT DROP would drop the table before the next statement.
CREATE TEMP TABLE seed_analytics_users (
    idx int PRIMARY KEY,
    user_id uuid NOT NULL,
    email text NOT NULL
);

INSERT INTO seed_analytics_users (idx, user_id, email)
SELECT
    g,
    ('a1000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'seed-analytics-' || lpad(g::text, 3, '0') || '@histar.vn'
FROM generate_series(1, 120) AS g;

-- Cleanup previous seed (FK-safe order)
DELETE FROM visit_session_events
WHERE visit_session_id IN (
    SELECT vs.id FROM visit_sessions vs
    JOIN seed_analytics_users u ON u.user_id = vs.user_id
);
DELETE FROM analytics_events
WHERE user_id IN (SELECT user_id FROM seed_analytics_users)
   OR metadata = 'seed-admin-analytics-cu-chi';
DELETE FROM visit_sessions
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM pilot_events
WHERE user_id IN (SELECT user_id FROM seed_analytics_users)
   OR payload_json LIKE '%"seed":"admin-analytics-cu-chi"%';
DELETE FROM checkins
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM user_discoveries
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM user_quest_progress
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM profiles
WHERE email LIKE 'seed-analytics-%@histar.vn'
   OR id IN (SELECT user_id FROM seed_analytics_users);

INSERT INTO profiles (id, email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at, email_verified)
SELECT
    u.user_id,
    u.email,
    crypt('Demo@2026', gen_salt('bf', 4)),
    'local',
    'USER',
    CASE WHEN u.idx <= 40 THEN 'PREMIUM' ELSE 'FREE' END,
    'Seed Analytics ' || lpad(u.idx::text, 3, '0'),
    1 + (u.idx % 8),
    100 + u.idx * 17,
    'TP.HCM',
    now() - ((u.idx % 25) || ' days')::interval,
    true
FROM seed_analytics_users u;

-- Closed visit sessions (~14â€“22 min) with discovery events for drop-off + session quality
WITH loc AS (
    SELECT '11111111-1111-1111-1111-111111111111'::uuid AS id
),
pois AS (
    SELECT unlock_key, name, sort_order,
           row_number() OVER (ORDER BY sort_order) AS rn
    FROM discovery_points
    WHERE location_id = '11111111-1111-1111-1111-111111111111'
      AND deleted_at IS NULL
),
sessions AS (
    INSERT INTO visit_sessions (
        id, user_id, location_id, mode, started_at, ended_at, status, last_activity_at, ended_reason
    )
    SELECT
        ('b2000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
        u.user_id,
        loc.id,
        CASE WHEN u.idx % 3 = 0 THEN 'onsite' ELSE 'online' END,
        now() - ((u.idx % 28) || ' days')::interval - interval '90 minutes',
        now() - ((u.idx % 28) || ' days')::interval - interval '90 minutes'
            + ((14 + (u.idx % 9)) || ' minutes')::interval,
        'CLOSED',
        now() - ((u.idx % 28) || ' days')::interval - interval '70 minutes',
        'COMPLETED'
    FROM seed_analytics_users u
    CROSS JOIN loc
    RETURNING id, user_id
)
INSERT INTO visit_session_events (id, visit_session_id, event_type, event_key, source, created_at, poi_name_snapshot)
SELECT
    gen_random_uuid(),
    s.id,
    'discovery',
    p.unlock_key,
    'seed-admin-analytics',
    now() - ((su.idx % 28) || ' days')::interval - interval '80 minutes' + (p.rn || ' minutes')::interval,
    p.name
FROM sessions s
JOIN seed_analytics_users su ON su.user_id = s.user_id
JOIN pois p ON p.rn <= CASE
    WHEN su.idx <= 100 THEN 6
    WHEN su.idx <= 110 THEN 4
    ELSE 2
END;

-- user_discoveries (POI unlock ranking) â€” top POIs get more distinct users
INSERT INTO user_discoveries (user_id, discovery_key, discovered_at, location_id)
SELECT
    u.user_id,
    p.unlock_key,
    now() - ((u.idx % 27) || ' days')::interval - (p.sort_order || ' hours')::interval,
    '11111111-1111-1111-1111-111111111111'::uuid
FROM seed_analytics_users u
JOIN discovery_points p
  ON p.location_id = '11111111-1111-1111-1111-111111111111'
 AND p.deleted_at IS NULL
WHERE
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222222' AND u.idx <= 110) OR -- Cá»•ng vÃ o ~92%
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222221' AND u.idx <= 95) OR  -- Báº¿p
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222223' AND u.idx <= 82) OR  -- PhÃ²ng há»p
    (p.unlock_key = 'photo:cua-ham' AND u.idx <= 70) OR
    (p.unlock_key = 'photo:gieng' AND u.idx <= 58) OR
    (p.unlock_key = 'artifact:vu-khi' AND u.idx <= 48) OR
    (p.unlock_key = 'artifact:tram-xa' AND u.idx <= 40) OR
    (p.unlock_key = 'hotspot:vent' AND u.idx <= 28) OR
    (p.unlock_key = 'artifact:bay' AND u.idx <= 22);

-- Quest funnel (started vs completed) â€” rates ~100 / 86 / 71 / 68 / 65 / 60 / 55 of base 120
INSERT INTO user_quest_progress (
    id, user_id, quest_id, status, started_at, completed_at, location_id, current_step, steps_total
)
SELECT
    gen_random_uuid(),
    u.user_id,
    q.id,
    CASE
        WHEN u.idx <= q.complete_upto THEN 'COMPLETED'
        WHEN u.idx <= q.start_upto THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END,
    CASE WHEN u.idx <= q.start_upto
        THEN now() - ((u.idx % 26) || ' days')::interval ELSE NULL END,
    CASE WHEN u.idx <= q.complete_upto
        THEN now() - ((u.idx % 26) || ' days')::interval + interval '40 minutes' ELSE NULL END,
    '11111111-1111-1111-1111-111111111111'::uuid,
    CASE WHEN u.idx <= q.complete_upto THEN 3 WHEN u.idx <= q.start_upto THEN 1 ELSE 0 END,
    3
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('33333333-3333-3333-3333-333333333335'::uuid, 120, 104), -- Ãnh sÃ¡ng ÄÃ¨n MÃ¹ U
        ('33333333-3333-3333-3333-333333333339'::uuid, 110, 86),  -- Ba táº§ng lÃ²ng Ä‘áº¥t
        ('33333333-3333-3333-3333-333333333337'::uuid, 100, 72),  -- Báº¿p khÃ´ng khÃ³i
        ('33333333-3333-3333-3333-333333333333'::uuid, 95, 65),   -- HÃ nh trÃ¬nh dÆ°á»›i lÃ²ng Ä‘áº¥t
        ('33333333-3333-3333-3333-333333333336'::uuid, 90, 58),   -- Máº­t lá»‡nh thá»­ game
        ('33333333-3333-3333-3333-333333333334'::uuid, 85, 52),   -- Máº­t mÃ£ Sa bÃ n
        ('33333333-3333-3333-3333-333333333338'::uuid, 80, 45)    -- Náº¯p háº§m vÃ  cáº¡m báº«y
) AS q(id, start_upto, complete_upto)
WHERE u.idx <= q.start_upto;

-- Online â†’ onsite conversion (~32% of discoverers also check in after discovery)
INSERT INTO checkins (id, user_id, location_id, latitude, longitude, created_at, presence_method)
SELECT
    ('c3000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    11.1438 + (u.idx * 0.00001),
    106.4631 + (u.idx * 0.00001),
    now() - ((u.idx % 25) || ' days')::interval + interval '2 hours',
    'GPS'
FROM seed_analytics_users u
WHERE u.idx <= 38;

-- analytics_events for KPI activeUsers / eventCount / activity-series / quest_completed
INSERT INTO analytics_events (
    id, user_id, location_id, visit_session_id, event_type, event_key, source, created_at, metadata
)
SELECT
    gen_random_uuid(),
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    ('b2000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
    e.event_type,
    e.event_key,
    'seed-admin-analytics',
    -- Bias weekends (Sat/Sun) higher density like Stitch heatmap
    date_trunc('day', now()) - ((u.idx + e.day_offset) % 28 || ' days')::interval
        + ((9 + (u.idx % 7)) || ' hours')::interval
        + ((u.idx % 50) || ' minutes')::interval,
    'seed-admin-analytics-cu-chi'
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('session_start', 'visit', 0),
        ('session_end', 'visit', 0),
        ('discovery', 'poi', 1),
        ('quest_started', 'quest', 2),
        ('quest_completed', 'quest', 3),
        ('checkin', 'gps', 4)
) AS e(event_type, event_key, day_offset)
WHERE NOT (e.event_type = 'quest_completed' AND u.idx > 78)
  AND NOT (e.event_type = 'checkin' AND u.idx > 38);

-- Extra weekdayÃ—hour events for Stitch heatmap peaks (09:30 / 14:30) and weekend denser cells
INSERT INTO analytics_events (
    id, user_id, location_id, event_type, event_key, source, created_at, metadata
)
SELECT
    gen_random_uuid(),
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    'session_start',
    'visit',
    'seed-admin-analytics',
    date_trunc('week', now())
      + ((d.dow) || ' days')::interval
      + ((h.hour) || ' hours')::interval
      + ((u.idx % 40) || ' minutes')::interval,
    'seed-admin-analytics-cu-chi'
FROM seed_analytics_users u
CROSS JOIN (VALUES (0), (1), (2), (3), (4), (5), (6)) AS d(dow)
CROSS JOIN (VALUES (8), (9), (10), (11), (13), (14), (15), (16)) AS h(hour)
WHERE u.idx <= CASE
    WHEN h.hour IN (9, 10, 14, 15) AND d.dow IN (5, 6) THEN 110
    WHEN h.hour IN (9, 10, 14, 15) THEN 85
    WHEN d.dow IN (5, 6) THEN 55
    ELSE 28
END;

-- Pilot KPI attribution (last 7 days) for "Nguá»“n thu hÃºt"
INSERT INTO pilot_events (id, client_uuid, user_id, session_id, event_type, station_code, payload_json, occurred_at, created_at)
SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    u.user_id,
    gen_random_uuid(),
    e.event_type,
    NULL,
    '{"seed":"admin-analytics-cu-chi"}',
    now() - ((u.idx % 6) || ' days')::interval - ((u.idx % 8) || ' hours')::interval,
    now()
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('landing_visit', 90),
        ('pack_loaded', 70),
        ('share_initiated', 35),
        ('checkin_result', 38),
        ('nps_submitted', 25),
        ('session_start', 100),
        ('session_end', 95)
) AS e(event_type, max_idx)
WHERE u.idx <= e.max_idx;

-- END migration: V72__admin_analytics_cu_chi_demo_seed.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V73__admin_audit_client_ip_location_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V73__admin_audit_client_ip_location_seed.sql
-- ============================================================================
-- Backfill clientIp + location into existing admin_audit_events metadata (demo / ops).
-- Skips rows that already have clientIp or ip. Cycles a small VN ISP sample set.

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY occurred_at DESC, id) AS rn
    FROM admin_audit_events
    WHERE metadata_json IS NULL
       OR NOT (
            COALESCE(metadata_json::jsonb, '{}'::jsonb) ? 'clientIp'
            OR COALESCE(metadata_json::jsonb, '{}'::jsonb) ? 'ip'
       )
),
sample AS (
    SELECT *
    FROM (VALUES
        (0, '14.232.18.91',  'Hà Nội, VN'),
        (1, '118.70.211.42', 'Hà Nội, VN'),
        (2, '42.116.89.15',  'Đà Nẵng, VN'),
        (3, '113.161.72.8',  'TP. Hồ Chí Minh, VN'),
        (4, '171.224.178.33','Hà Nội, VN'),
        (5, '27.72.144.10',  'Hải Phòng, VN'),
        (6, '115.73.214.55', 'Cần Thơ, VN'),
        (7, '203.113.131.7', 'TP. Hồ Chí Minh, VN')
    ) AS t(slot, client_ip, location)
)
UPDATE admin_audit_events e
SET metadata_json = (
    COALESCE(e.metadata_json::jsonb, '{}'::jsonb)
    || jsonb_build_object(
        'clientIp', s.client_ip,
        'location', s.location
    )
)::text
FROM ranked r
JOIN sample s ON s.slot = ((r.rn - 1) % 8)
WHERE e.id = r.id;

-- END migration: V73__admin_audit_client_ip_location_seed.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V74__profiles_account_status.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V74__profiles_account_status.sql
-- ============================================================================
-- Admin IAM P0: suspend / unsuspend accounts
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(20);

UPDATE profiles
SET account_status = 'ACTIVE'
WHERE account_status IS NULL OR BTRIM(account_status) = '';

ALTER TABLE profiles
    ALTER COLUMN account_status SET DEFAULT 'ACTIVE';

ALTER TABLE profiles
    ALTER COLUMN account_status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_profiles_account_status'
    ) THEN
        ALTER TABLE profiles
            ADD CONSTRAINT chk_profiles_account_status
            CHECK (UPPER(account_status) IN ('ACTIVE', 'SUSPENDED'));
    END IF;
END $$;

-- END migration: V74__profiles_account_status.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V75__organization_ops_extensions.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V75__organization_ops_extensions.sql
-- ============================================================================
-- Admin Organizations P0: contracts, AR cards, certificates, CRM link

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS last_crm_synced_at TIMESTAMPTZ;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS crm_linked_inquiry_count INT NOT NULL DEFAULT 0;

ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_heritage_digitization_inquiries_org
    ON heritage_digitization_inquiries (organization_id);

CREATE TABLE IF NOT EXISTS organization_contracts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    contract_code VARCHAR(64) NOT NULL,
    license_seats INT,
    plan_type VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    starts_on DATE,
    ends_on DATE,
    document_url TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES profiles(id),
    CONSTRAINT uq_org_contracts_org_code UNIQUE (organization_id, contract_code),
    CONSTRAINT chk_org_contracts_status CHECK (UPPER(status) IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_organization_contracts_org
    ON organization_contracts (organization_id);

CREATE TABLE IF NOT EXISTS organization_ar_cards (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    card_code VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    issued_by UUID REFERENCES profiles(id),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_org_ar_cards_status CHECK (UPPER(status) IN ('ACTIVE', 'REVOKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_ar_cards_active_member
    ON organization_ar_cards (organization_id, user_id)
    WHERE UPPER(status) = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_organization_ar_cards_org
    ON organization_ar_cards (organization_id);

CREATE TABLE IF NOT EXISTS organization_certificates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    certificate_code VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    issued_by UUID REFERENCES profiles(id),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_organization_certificates_org
    ON organization_certificates (organization_id);

-- Optional demo contracts for first two orgs (idempotent by code)
INSERT INTO organization_contracts (
    id, organization_id, title, contract_code, license_seats, plan_type, status,
    starts_on, ends_on, notes, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    o.id,
    'Hợp đồng trải nghiệm AR HistAR',
    'CTR-DEMO-' || UPPER(SUBSTRING(REPLACE(o.id::text, '-', ''), 1, 6)),
    COALESCE(o.max_verified_accounts, 30),
    COALESCE(o.plan_type, o.plan, 'STANDARD'),
    'ACTIVE',
    CURRENT_DATE - 30,
    CURRENT_DATE + 335,
    'Seed hợp đồng demo (V75)',
    NOW(),
    NOW()
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM organization_contracts c
    WHERE c.organization_id = o.id AND c.contract_code LIKE 'CTR-DEMO-%'
)
ORDER BY o.created_at NULLS LAST, o.name
LIMIT 2;

-- END migration: V75__organization_ops_extensions.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V76__admin_audit_hash_chain.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V76__admin_audit_hash_chain.sql
-- ============================================================================
-- Admin audit SHA-256 hash chain (prev + event hash).
-- Row hashes are backfilled by AdminAuditHashBackfill on startup so Instant formatting matches Java verify.

ALTER TABLE admin_audit_events
    ADD COLUMN IF NOT EXISTS event_hash VARCHAR(64);

ALTER TABLE admin_audit_events
    ADD COLUMN IF NOT EXISTS prev_event_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_admin_audit_events_occurred_id
    ON admin_audit_events (occurred_at ASC, id ASC);

-- END migration: V76__admin_audit_hash_chain.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V77__location_ar_readiness_seed.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V77__location_ar_readiness_seed.sql
-- ============================================================================
-- V77: Seed is_ar_available for all 10 heritage locations.
-- Column was added via Hibernate DDL; this migration sets the authoritative values.
-- Admin can toggle individual sites via PATCH /api/admin/locations/{id} (W1.2).
--
-- READY (has panoramas + stations + quest content):
--   Củ Chi (11111111-...) — full content, panoramas, minigames
--   Hoàng Thành Thăng Long (204) — content seeded in narrative pilots
--
-- NOT READY (content pending — see Wave 3 of GO_TO_MARKET_PLAN):
--   All others — enable via admin console when content is complete.

UPDATE locations
SET is_ar_available = TRUE
WHERE id IN (
    '11111111-1111-1111-1111-111111111111',  -- Địa Đạo Củ Chi
    '22222222-2222-2222-2222-222222222204'   -- Hoàng Thành Thăng Long
);

UPDATE locations
SET is_ar_available = FALSE
WHERE id IN (
    '22222222-2222-2222-2222-222222222209',  -- Đền Hùng Vương
    '22222222-2222-2222-2222-222222222207',  -- Văn Miếu Quốc Tử Giám
    '22222222-2222-2222-2222-222222222203',  -- Cố Đô Hoa Lư
    '22222222-2222-2222-2222-222222222206',  -- Thành Nhà Hồ
    '22222222-2222-2222-2222-222222222202',  -- Chùa Thiên Mụ
    '22222222-2222-2222-2222-222222222208',  -- Đại Nội Huế
    '22222222-2222-2222-2222-222222222205'   -- Phố Cổ Hội An
)
  AND (is_ar_available IS NULL OR is_ar_available = FALSE);
-- Note: 10th location (Dinh Độc Lập / Ben Nha Rong) — not seeded in V8, check before enabling.

-- END migration: V77__location_ar_readiness_seed.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V78__station_latlng_cuuchi_placeholder.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V78__station_latlng_cuuchi_placeholder.sql
-- ============================================================================
-- V78: Placeholder GPS coordinates for Củ Chi pilot stations ST01–ST06.
--
-- ⚠️  PLACEHOLDER — cần đo thực địa và thay bằng tọa độ chính xác.
-- Các tọa độ này nằm trong khuôn viên khu di tích Địa đạo Củ Chi (Ben Duoc)
-- và được bố trí dọc tuyến tham quan ~200m.
-- Sau khi field test: cập nhật lại bằng migration V79 hoặc UPDATE thủ công qua admin.
--
-- Ref GPS trung tâm khu Ben Duoc: 11.1416°N, 106.4616°E

UPDATE stations SET lat = 11.14155, lng = 106.46135
WHERE site_code = 'cu-chi' AND code = 'ST01'; -- Phòng họp (khu đón tiếp)

UPDATE stations SET lat = 11.14148, lng = 106.46148
WHERE site_code = 'cu-chi' AND code = 'ST02'; -- Kho ngầm

UPDATE stations SET lat = 11.14140, lng = 106.46162
WHERE site_code = 'cu-chi' AND code = 'ST03'; -- Giếng hầm & thông gió

UPDATE stations SET lat = 11.14133, lng = 106.46175
WHERE site_code = 'cu-chi' AND code = 'ST04'; -- Quân y

UPDATE stations SET lat = 11.14126, lng = 106.46188
WHERE site_code = 'cu-chi' AND code = 'ST05'; -- Xưởng vũ khí

UPDATE stations SET lat = 11.14119, lng = 106.46200
WHERE site_code = 'cu-chi' AND code = 'ST06'; -- Hầm phòng thủ (điểm cuối)

-- END migration: V78__station_latlng_cuuchi_placeholder.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V79__seed_platform_accounts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V79__seed_platform_accounts.sql
-- ============================================================================
-- Seed platform accounts (admin, demo, teacher) via Flyway so they survive DB resets.
-- Uses pgcrypto crypt() with bcrypt (bf, cost 10) to hash Demo@2026.
-- Idempotent: ON CONFLICT DO UPDATE ensures re-runnable.
-- These accounts are dev/demo only — rotate passwords on production.

-- Ensure pgcrypto extension is available (needed for crypt + gen_salt)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO profiles (email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at, email_verified, email_verified_at)
VALUES
    ('admin@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'ADMIN', 'FREE', 'Platform Admin',
     1, 0, 'TP.HCM', now(), true, now()),
    ('demo@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'ADMIN', 'FREE', 'Demo Admin',
     10, 999, 'TP.HCM', now(), true, now()),
    ('teacher@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'TEACHER', 'FREE', 'Giáo viên Demo',
     1, 0, 'TP.HCM', now(), true, now())
ON CONFLICT (email) DO UPDATE
    SET password_hash      = crypt('Demo@2026', gen_salt('bf', 10)),
        role               = EXCLUDED.role,
        email_verified     = TRUE,
        email_verified_at  = COALESCE(profiles.email_verified_at, now());

-- END migration: V79__seed_platform_accounts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V80__cu_chi_explore_archive_points.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V80__cu_chi_explore_archive_points.sql
-- ============================================================================
-- src/main/resources/db/migration/V68__cu_chi_explore_archive_points.sql
-- Đăng ký các điểm giải mã Mật thư, Hồ sơ câu chuyện, Bàn điều tra & Mặt cắt độ sâu cho Địa đạo Củ Chi

INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
-- 1. Ba Mật thư Chiến khu (Ciphers)
('d0000001-0000-4000-8000-000000000101', '11111111-1111-1111-1111-111111111111', 'Mật lệnh số 19-HC: Bếp Hoàng Cầm', 32.00, 38.00, 'cipher:hoang-cam', 101),
('d0000001-0000-4000-8000-000000000102', '11111111-1111-1111-1111-111111111111', 'Mật thư kỹ thuật 68-ĐH: Đào hầm 2 giếng', 35.00, 40.00, 'cipher:two-wells', 102),
('d0000001-0000-4000-8000-000000000103', '11111111-1111-1111-1111-111111111111', 'Mật lệnh trinh sát 07-DR: Dép râu ngược', 40.00, 45.00, 'cipher:sandals', 103),

-- 2. Sáu Hồ sơ Mảnh chuyện Đất Thép (Story Dossiers)
('d0000001-0000-4000-8000-000000000104', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-01: Kỳ quan 250km Thành phố ngầm', 36.00, 42.00, 'dossier:architecture', 104),
('d0000001-0000-4000-8000-000000000105', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-02: Trận địa chông mìn tự tạo', 44.00, 48.00, 'dossier:trap-warfare', 105),
('d0000001-0000-4000-8000-000000000106', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-03: Đời sống dưới hầm sâu 6m', 38.00, 36.00, 'dossier:underground-life', 106),
('d0000001-0000-4000-8000-000000000107', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-04: Sở chỉ huy ngầm Mậu Thân 1968', 48.00, 52.00, 'dossier:command-center', 107),
('d0000001-0000-4000-8000-000000000108', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-05: Tư liệu đối chứng Tunnel Rats', 52.00, 55.00, 'dossier:tunnel-rats', 108),
('d0000001-0000-4000-8000-000000000109', '11111111-1111-1111-1111-111111111111', 'Hồ sơ HS-06: Đền tưởng niệm Bến Dược', 28.00, 30.00, 'dossier:ben-duoc', 109),

-- 3. Bốn Mảnh chứng cứ & Kết luận Bàn điều tra (Investigation Board)
('d0000001-0000-4000-8000-000000000110', '11111111-1111-1111-1111-111111111111', 'Chứng cứ EV-01: Vết cắt vát chữ V nắp hầm', 33.00, 39.00, 'clue:ev-01', 110),
('d0000001-0000-4000-8000-000000000111', '11111111-1111-1111-1111-111111111111', 'Chứng cứ EV-02: Ống thông hơi ụ mối', 37.00, 41.00, 'clue:ev-02', 111),
('d0000001-0000-4000-8000-000000000112', '11111111-1111-1111-1111-111111111111', 'Chứng cứ EV-03: Báo cáo hành quân Crimp', 41.00, 43.00, 'clue:ev-03', 112),
('d0000001-0000-4000-8000-000000000113', '11111111-1111-1111-1111-111111111111', 'Chứng cứ EV-04: Bố cục liên hoàn ấp', 45.00, 47.00, 'clue:ev-04', 113),
('d0000001-0000-4000-8000-000000000114', '11111111-1111-1111-1111-111111111111', 'Kết luận điều tra: Hệ thống ngụy trang Củ Chi', 46.00, 49.00, 'investigation:cu-chi-conclusion', 114),

-- 4. Bốn Tầng mặt cắt độ sâu (Depth Cross-Section) - Đã chuyển thành 0 XP (Chỉ là hành động xem)
('d0000001-0000-4000-8000-000000000115', '11111111-1111-1111-1111-111111111111', 'Khảo sát độ sâu 0m: Mặt đất ngụy trang', 30.00, 32.00, 'depth:surface', 0),
('d0000001-0000-4000-8000-000000000116', '11111111-1111-1111-1111-111111111111', 'Khảo sát độ sâu -3m: Tầng 1 chiến đấu & sinh hoạt', 30.00, 34.00, 'depth:layer1', 0),
('d0000001-0000-4000-8000-000000000117', '11111111-1111-1111-1111-111111111111', 'Khảo sát độ sâu -6m: Tầng 2 y tế & chỉ huy', 30.00, 36.00, 'depth:layer2', 0),
('d0000001-0000-4000-8000-000000000118', '11111111-1111-1111-1111-111111111111', 'Khảo sát độ sâu -10m: Tầng 3 đáy sinh tồn', 30.00, 38.00, 'depth:layer3', 0),

-- 5. Bốn Thử thách Trắc nghiệm của Tầng ngầm (Depth Quizzes) - Trả lời đúng mới nhận 15 XP
('d0000001-0000-4000-8000-000000000119', '11111111-1111-1111-1111-111111111111', 'Thử thách: Mùi tiêu và chó nghiệp vụ', 30.00, 33.00, 'depth_quiz:surface', 15),
('d0000001-0000-4000-8000-000000000120', '11111111-1111-1111-1111-111111111111', 'Thử thách: Nút chặn chữ Z', 30.00, 35.00, 'depth_quiz:layer1', 15),
('d0000001-0000-4000-8000-000000000121', '11111111-1111-1111-1111-111111111111', 'Thử thách: Bẫy nước chống khí độc', 30.00, 37.00, 'depth_quiz:layer2', 15),
('d0000001-0000-4000-8000-000000000122', '11111111-1111-1111-1111-111111111111', 'Thử thách: Kết cấu đất đá ong', 30.00, 39.00, 'depth_quiz:layer3', 15)
    ON CONFLICT (location_id, unlock_key) DO NOTHING;

-- END migration: V80__cu_chi_explore_archive_points.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V81__reward_integrity.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V81__reward_integrity.sql
-- ============================================================================
-- Historical totals are retained as an explicitly unverified opening balance.
ALTER TABLE messages ADD COLUMN IF NOT EXISTS server_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- Viewing the Cedar Falls diorama is distinct from finding the 360 tour's sa-ban fragment.
INSERT INTO discovery_points(id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
VALUES ('d0000001-0000-4000-8000-000000000123', '11111111-1111-1111-1111-111111111111',
    'Trải nghiệm sa bàn Cedar Falls (không thưởng XP)', 50, 50, 'feature:diorama', 0)
ON CONFLICT(location_id, unlock_key) DO NOTHING;

-- Existing local databases may already contain Hibernate-created tables. Adopt them
-- without dropping records; defaults and constraints are installed below as well.
CREATE TABLE IF NOT EXISTS xp_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    points INTEGER NOT NULL,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, event_key)
);
ALTER TABLE xp_ledger ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE xp_ledger ALTER COLUMN awarded_at SET DEFAULT now();

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass
        AND contype = 'u' AND pg_get_constraintdef(oid) = 'UNIQUE (user_id, event_key)') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_user_event_unique UNIQUE(user_id, event_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass AND conname = 'xp_ledger_points_nonnegative') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_points_nonnegative CHECK(points >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass AND conname = 'xp_ledger_user_fk') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_user_fk FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
    -- A missing opening balance may be reconstructed only if journal entries
    -- do not already exceed the account total. Never silently rewrite a total.
    IF EXISTS (SELECT 1 FROM profiles p WHERE NOT EXISTS (
        SELECT 1 FROM xp_ledger x WHERE x.user_id = p.id AND x.event_key = 'baseline:v1')
        AND COALESCE((SELECT SUM(x.points) FROM xp_ledger x WHERE x.user_id = p.id), 0)
            > GREATEST(COALESCE(p.total_points, 0), 0)) THEN
        RAISE EXCEPTION 'XP journal exceeds account total; reconcile historical data before V69.';
    END IF;
END $$;

INSERT INTO xp_ledger(id, user_id, event_key, points, awarded_at)
SELECT gen_random_uuid(), p.id, 'baseline:v1',
    GREATEST(COALESCE(p.total_points, 0), 0) -
        COALESCE((SELECT SUM(x.points) FROM xp_ledger x WHERE x.user_id = p.id), 0), now()
FROM profiles p
WHERE NOT EXISTS (SELECT 1 FROM xp_ledger x WHERE x.user_id = p.id AND x.event_key = 'baseline:v1')
ON CONFLICT(user_id, event_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS explore_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    challenge_key VARCHAR(128) NOT NULL,
    answer INTEGER,
    passed BOOLEAN NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE explore_attempts ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE explore_attempts ALTER COLUMN submitted_at SET DEFAULT now();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_answer_range') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_answer_range CHECK(answer BETWEEN 0 AND 3);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_user_fk') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_user_fk FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_location_fk') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_location_fk FOREIGN KEY(location_id) REFERENCES locations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema()
        AND tablename = 'explore_attempts' AND indexdef LIKE '% USING btree (user_id, submitted_at)') THEN
        CREATE INDEX idx_explore_attempts_user_time ON explore_attempts(user_id, submitted_at);
    END IF;
END $$;

INSERT INTO badges(id, name, description, icon_url, condition_type, condition_value, location_id) VALUES
('b0000001-0000-4000-8000-000000000201', 'La Bàn Khởi Nguyên', '50 XP toàn tài khoản hoặc hoàn thành một thử thách tầng.', '/badges/badge-2.png', 'explore', 1, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000202', 'Dấu Chân Đất Thép', 'Hoàn thành bốn thử thách tầng.', '/badges/badge-5.png', 'explore', 2, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000203', 'Bậc Thầy Cơ Yếu', 'Giải ba mật thư.', '/badges/badge-8.png', 'explore', 3, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000204', 'Thám Tử Sử Liệu', 'Bốn chứng cứ và kết luận được server xác nhận.', '/badges/badge-6.png', 'explore', 4, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000205', 'Tri Kỷ Tiền Nhân', 'Có hội thoại AI, 300 XP toàn tài khoản và hai hồ sơ.', '/badges/badge-1.png', 'explore', 5, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000206', 'Người Giữ Kho Báu', 'Mở khóa cổ vật, 500 XP toàn tài khoản và ba mật thư.', '/badges/badge-7.png', 'explore', 6, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000207', 'Tinh Hoa Bác Học', 'Hoàn thành sáu hồ sơ.', '/badges/badge-3.png', 'explore', 7, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000208', 'Sứ Giả Thời Không', 'Có trải nghiệm 360°, sa bàn và 600 XP toàn tài khoản.', '/badges/badge-4.png', 'explore', 8, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000209', 'Nhãn Quan Xuyên Thấu', '1.130 XP toàn tài khoản, 360°, sa bàn, AI và mở khóa cổ vật.', '/badges/badge-10.png', 'explore', 9, '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- END migration: V81__reward_integrity.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V82__seed_demo_segment_accounts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V82__seed_demo_segment_accounts.sql
-- ============================================================================
-- Seed demo accounts for all 3 business segments (EXE201 demo readiness).
-- Password: Demo@2026 (bcrypt via pgcrypto).
-- Idempotent: ON CONFLICT DO UPDATE.
--
-- B2C Premium:  premium@histar.vn     (tier=PREMIUM, active b2c_subscription)
-- B2B Standard: org-standard@histar.vn (tier=FREE, org_id→Demo School Standard)
-- B2B Micro:    org-micro@histar.vn   (tier=FREE, org_id→Demo School Micro)
-- B2B Teacher:  teacher@histar.vn already exists (V79), updated with org link
-- Existing:     demo@histar.vn (ADMIN), admin@histar.vn (ADMIN) — kept as-is.
-- Mã mời: DEMOSTD1 (Standard), DEMOMIC1 (Micro) — organizations.invite_code là VARCHAR(8).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

---------------------------------------------------------------------------
-- 1. B2C Premium user
---------------------------------------------------------------------------
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at)
VALUES ('premium@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'USER', 'PREMIUM', 'Khách Premium Demo',
        5, 350, 'TP.HCM', now(), true, now())
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        tier              = 'PREMIUM',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- Active B2C subscription for premium user (30-day window)
INSERT INTO b2c_subscriptions (user_id, price_vnd, start_date, end_date, is_active, payment_method)
SELECT p.id, 79000, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', TRUE, 'DEMO'
FROM profiles p
WHERE p.email = 'premium@histar.vn'
  AND NOT EXISTS (
    SELECT 1 FROM b2c_subscriptions bs
    WHERE bs.user_id = p.id AND bs.is_active = TRUE
);

---------------------------------------------------------------------------
-- 2. B2B Organizations (Standard + Micro)
---------------------------------------------------------------------------
-- Demo School Standard
INSERT INTO organizations (slug, name, plan, plan_type, contact_email,
                           plan_start_date, plan_end_date, status,
                           max_ccu, max_verified_accounts, max_ai_queries_per_month,
                           invite_code, invite_code_expires_at, created_at)
SELECT 'demo-school-standard', 'Trường THPT Demo Standard', 'org_standard', 'STANDARD',
       'org-standard@histar.vn',
       CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 'ACTIVE',
       40, 400, 30000,
       'DEMOSTD1', now() + INTERVAL '365 days', now()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-school-standard');

UPDATE organizations
SET plan_type              = 'STANDARD',
    max_ccu                = 40,
    max_verified_accounts  = 400,
    max_ai_queries_per_month = 30000,
    plan_end_date          = CURRENT_DATE + INTERVAL '90 days',
    status                 = 'ACTIVE'
WHERE slug = 'demo-school-standard';

-- Demo School Micro
INSERT INTO organizations (slug, name, plan, plan_type, contact_email,
                           plan_start_date, plan_end_date, status,
                           max_ccu, max_verified_accounts, max_ai_queries_per_month,
                           invite_code, invite_code_expires_at, created_at)
SELECT 'demo-school-micro', 'Trường THPT Demo Micro', 'org_micro', 'MICRO',
       'org-micro@histar.vn',
       CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'ACTIVE',
       15, 100, 5000,
       'DEMOMIC1', now() + INTERVAL '365 days', now()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-school-micro');

UPDATE organizations
SET plan_type              = 'MICRO',
    max_ccu                = 15,
    max_verified_accounts  = 100,
    max_ai_queries_per_month = 5000,
    plan_end_date          = CURRENT_DATE + INTERVAL '30 days',
    status                 = 'ACTIVE'
WHERE slug = 'demo-school-micro';

---------------------------------------------------------------------------
-- 3. B2B Org member accounts
---------------------------------------------------------------------------
-- org-standard@histar.vn — org admin for Standard school
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at,
                      org_id, org_subscription)
VALUES ('org-standard@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'TEACHER', 'FREE', 'GV Demo Standard',
        3, 120, 'TP.HCM', now(), true, now(),
        (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
        'STANDARD')
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        role              = 'TEACHER',
        org_id            = (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
        org_subscription  = 'STANDARD',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- org-micro@histar.vn — org admin for Micro school
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at,
                      org_id, org_subscription)
VALUES ('org-micro@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'TEACHER', 'FREE', 'GV Demo Micro',
        1, 0, 'TP.HCM', now(), true, now(),
        (SELECT id FROM organizations WHERE slug = 'demo-school-micro'),
        'MICRO')
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        role              = 'TEACHER',
        org_id            = (SELECT id FROM organizations WHERE slug = 'demo-school-micro'),
        org_subscription  = 'MICRO',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- Link existing teacher@histar.vn to the Standard org
UPDATE profiles
SET org_id           = (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
    org_subscription = 'STANDARD'
WHERE email = 'teacher@histar.vn'
  AND org_id IS NULL;

---------------------------------------------------------------------------
-- 4. B2B subscription records
---------------------------------------------------------------------------
INSERT INTO subscriptions (organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method)
SELECT o.id, 'STANDARD', 2490000, CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', TRUE, 'DEMO'
FROM organizations o
WHERE o.slug = 'demo-school-standard'
  AND NOT EXISTS (
    SELECT 1 FROM subscriptions s
    WHERE s.organization_id = o.id AND s.is_active = TRUE
);

INSERT INTO subscriptions (organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method)
SELECT o.id, 'MICRO', 490000, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', TRUE, 'DEMO'
FROM organizations o
WHERE o.slug = 'demo-school-micro'
  AND NOT EXISTS (
    SELECT 1 FROM subscriptions s
    WHERE s.organization_id = o.id AND s.is_active = TRUE
);

-- END migration: V82__seed_demo_segment_accounts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V83__quest_tracks_v2_schema.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V83__quest_tracks_v2_schema.sql
-- ============================================================================
-- Nhiệm vụ v2 (Giai đoạn 1) — schema.
-- Spec: BE/docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md
--
-- quests: tách nhóm ONLINE / ONSITE, thứ tự hiển thị, độ khó, thời lượng.
-- quest_steps: bước bắt buộc / tùy chọn, bước Premium (luôn là tùy chọn).
-- Các cột cũ (step_discovery_keys, steps_total, completion_trigger, require_onsite_checkin,
-- required_order) được giữ một bản phát hành để tương thích; V84 đồng bộ chúng từ quest_steps.
-- Chạy lại an toàn (IF NOT EXISTS / kiểm tra pg_constraint).

ALTER TABLE quests ADD COLUMN IF NOT EXISTS track VARCHAR(16) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE quests ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 100;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS difficulty VARCHAR(16);
ALTER TABLE quests ADD COLUMN IF NOT EXISTS estimated_minutes INT;

ALTER TABLE quest_steps ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE quest_steps ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;

-- Thứ tự cũ (required_order 1, 2, 3…) → sort_order 10, 20, 30…; nhiệm vụ không có thứ tự giữ 100.
UPDATE quests
SET sort_order = required_order * 10
WHERE required_order IS NOT NULL
  AND required_order > 0
  AND sort_order = 100;

-- Bước cổng thời gian cần Premium đang có trong dữ liệu → tùy chọn (quyết định #2).
UPDATE quest_steps
SET is_premium = TRUE,
    is_required = FALSE
WHERE action_type = 'portal'
  AND portal_era IN (1948, 1968)
  AND (is_premium = FALSE OR is_required = TRUE);

-- Chuẩn hoá step_order trước khi thêm UNIQUE: chỉ những nhiệm vụ đang trùng hoặc thiếu thứ tự.
WITH broken AS (
    SELECT quest_id
    FROM quest_steps
    GROUP BY quest_id
    HAVING COUNT(*) <> COUNT(DISTINCT step_order)
),
ranked AS (
    SELECT s.id,
           ROW_NUMBER() OVER (PARTITION BY s.quest_id ORDER BY s.step_order NULLS LAST, s.id) AS rn
    FROM quest_steps s
    JOIN broken b ON b.quest_id = s.quest_id
)
UPDATE quest_steps s
SET step_order = r.rn
FROM ranked r
WHERE s.id = r.id
  AND s.step_order IS DISTINCT FROM r.rn;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quests_track') THEN
        ALTER TABLE quests
            ADD CONSTRAINT chk_quests_track CHECK (track IN ('ONLINE', 'ONSITE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quests_difficulty') THEN
        ALTER TABLE quests
            ADD CONSTRAINT chk_quests_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quest_steps_premium_optional') THEN
        ALTER TABLE quest_steps
            ADD CONSTRAINT chk_quest_steps_premium_optional CHECK (NOT (is_premium AND is_required));
    END IF;
    -- DEFERRABLE: V84 và trình sửa bước sau này đổi thứ tự nhiều bước trong cùng một giao dịch.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_quest_steps_quest_order') THEN
        ALTER TABLE quest_steps
            ADD CONSTRAINT uq_quest_steps_quest_order UNIQUE (quest_id, step_order) DEFERRABLE INITIALLY DEFERRED;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_quests_location_track_sort ON quests (location_id, track, sort_order);

-- END migration: V83__quest_tracks_v2_schema.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V84__quest_tracks_v2_content.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V84__quest_tracks_v2_content.sql
-- ============================================================================
-- Nhiệm vụ v2 (Giai đoạn 1) — nội dung.
-- Spec: BE/docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md
--
-- • quest_steps là nguồn định nghĩa duy nhất. Mọi bước có id cố định, upsert theo (quest_id, unlock_key).
-- • Củ Chi ONLINE: 333 → (337, 338, 339 mở bằng 2 cảnh tour) → 334 → 335. Bước cổng 1948/1968 là tùy chọn Premium.
-- • Củ Chi ONSITE (mới): 340 "Theo dấu địa đạo" — quét QR ở 6 trạm ST01–ST06 (tên trạm lấy từ bảng stations).
-- • 9 di tích khác: bỏ bước era:2026 (Cổng thời gian của các di tích này đang được cập nhật).
-- • Không xoá nhiệm vụ, không sửa user_quest_progress / user_discoveries / checkins.
--   Người đang làm dở được hoàn thành qua POST /api/v2/me/quests/reconcile.
-- Chạy lại an toàn.

-- ---------------------------------------------------------------------------
-- 1. Điểm khám phá cho các khóa mới (BE từ chối khóa không có trong discovery_points)
-- ---------------------------------------------------------------------------
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
SELECT v.id::uuid, '11111111-1111-1111-1111-111111111111'::uuid, v.name, v.x, v.y, v.unlock_key, v.sort_order
FROM (VALUES
    ('d0000001-0000-4000-8000-000000000010', '1948 — khởi đầu hầm ngầm', 50.00, 18.00, 'era:1948', 10),
    ('d0000001-0000-4000-8000-000000000011', 'Tết Mậu Thân 1968', 45.00, 25.00, 'era:1968', 11),
    ('d0000001-0000-4000-8000-000000000012', 'Khu di tích 2026', 55.00, 82.00, 'era:2026', 12),
    ('d0000001-0000-4000-8000-000000000060', 'Xưa & Nay · Miệng hầm', 48.00, 66.00, 'xuanay:mieng-ham', 60),
    ('d0000001-0000-4000-8000-000000000061', 'Xưa & Nay · Bếp Hoàng Cầm', 35.00, 49.00, 'xuanay:bep-hoang-cam', 61),
    ('d0000001-0000-4000-8000-000000000062', 'Xưa & Nay · Phòng họp dưới lòng đất', 62.00, 44.00, 'xuanay:phong-hop', 62),
    ('d0000001-0000-4000-8000-000000000063', 'Xưa & Nay · Lỗ thông hơi ụ mối', 41.00, 38.00, 'xuanay:thong-hoi', 63),
    ('d0000001-0000-4000-8000-000000000064', 'Xưa & Nay · Ánh sáng trong hầm', 56.00, 47.00, 'xuanay:den-ham', 64)
) AS v(id, name, x, y, unlock_key, sort_order)
WHERE EXISTS (SELECT 1 FROM locations l WHERE l.id = '11111111-1111-1111-1111-111111111111')
ON CONFLICT DO NOTHING;

-- ---------------------------------------------------------------------------
-- 2. Nhiệm vụ Củ Chi
-- ---------------------------------------------------------------------------
-- 334 và 335 trước đây chỉ có trong seed tay; upsert để DB nào cũng có.
INSERT INTO quests (id, location_id, title, description, story, points_reward, cover_image,
                    completion_trigger, steps_total, step_discovery_keys)
SELECT v.id::uuid, '11111111-1111-1111-1111-111111111111'::uuid, v.title, v.description, v.story, v.points,
       '/media/heritage/dia-dao-cu-chi/cover.jpeg', 'discovery', 3, NULL
FROM (VALUES
    ('33333333-3333-3333-3333-333333333334', 'Mật mã Sa bàn Tác chiến',
     'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.',
     'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".',
     150),
    ('33333333-3333-3333-3333-333333333335', 'Ánh sáng Đèn Mù U',
     'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.',
     'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.',
     120)
) AS v(id, title, description, story, points)
WHERE EXISTS (SELECT 1 FROM locations l WHERE l.id = '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- 340 — nhiệm vụ Tại di tích (mới).
INSERT INTO quests (id, location_id, title, description, story, points_reward, cover_image,
                    completion_trigger, steps_total, require_onsite_checkin, track)
SELECT '33333333-3333-3333-3333-333333333340'::uuid, '11111111-1111-1111-1111-111111111111'::uuid,
       'Theo dấu địa đạo',
       'Có mặt tại Địa đạo Củ Chi và quét mã QR ở 6 trạm trên lộ trình tham quan.',
       'Mỗi trạm là một mảnh của cuộc sống dưới lòng đất. Đi hết lộ trình để ghép lại cả hành trình.',
       150, '/media/heritage/dia-dao-cu-chi/cover.jpeg', 'checkin', 6, TRUE, 'ONSITE'
WHERE EXISTS (SELECT 1 FROM locations l WHERE l.id = '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;

-- Nhóm, thứ tự, độ khó, điều kiện mở. Chuỗi chính chỉ dùng unlock_after_quest_id (BE kiểm tra);
-- 337–339 chỉ cần 2 cảnh tour (unlock_discovery_keys), không cần hoàn thành 333.
UPDATE quests q
SET track = v.track,
    sort_order = v.sort_order,
    difficulty = v.difficulty,
    estimated_minutes = v.minutes,
    unlock_after_quest_id = v.after_id::uuid
FROM (VALUES
    ('33333333-3333-3333-3333-333333333333', 'ONLINE', 10, 'HARD', 25, NULL),
    ('33333333-3333-3333-3333-333333333337', 'ONLINE', 20, 'EASY', 8, NULL),
    ('33333333-3333-3333-3333-333333333338', 'ONLINE', 30, 'EASY', 10, NULL),
    ('33333333-3333-3333-3333-333333333339', 'ONLINE', 40, 'EASY', 8, NULL),
    ('33333333-3333-3333-3333-333333333334', 'ONLINE', 50, 'HARD', 20, '33333333-3333-3333-3333-333333333333'),
    ('33333333-3333-3333-3333-333333333335', 'ONLINE', 60, 'MEDIUM', 15, '33333333-3333-3333-3333-333333333334'),
    ('33333333-3333-3333-3333-333333333336', 'ONLINE', 900, 'EASY', 10, NULL),
    ('33333333-3333-3333-3333-333333333340', 'ONSITE', 10, 'MEDIUM', 45, NULL)
) AS v(id, track, sort_order, difficulty, minutes, after_id)
WHERE q.id = v.id::uuid;

UPDATE quests
SET description = 'Đi tour 360° qua Bếp Hoàng Cầm và Phòng họp, rồi so sánh Bếp Hoàng Cầm xưa và nay. Có Premium: xem thêm chương 1948 trong Cổng thời gian.'
WHERE id = '33333333-3333-3333-3333-333333333333'
  AND description LIKE '%check-in%';

-- ---------------------------------------------------------------------------
-- 3. Bước của nhiệm vụ Củ Chi
-- ---------------------------------------------------------------------------
-- 333 bỏ bước check-in (nhiệm vụ online không đòi có mặt — quyết định #1).
DELETE FROM quest_steps
WHERE quest_id = '33333333-3333-3333-3333-333333333333'
  AND (action_type = 'checkin' OR unlock_key LIKE 'checkin:%');

-- 334/335: bước cũ có id ngẫu nhiên (seed tay) → đổi sang id cố định. Không có khóa ngoại nào trỏ tới
-- quest_steps.id; discovery_content_bindings.quest_step_id (cột tùy chọn, không ràng buộc) được đổi kèm.
WITH fixed(quest_id, unlock_key, new_id) AS (
    VALUES
        ('33333333-3333-3333-3333-333333333334'::uuid, 'dialogue:anh-ba', '33333333-3333-3333-3333-333333333341'::uuid),
        ('33333333-3333-3333-3333-333333333334'::uuid, 'tour:sa-ban', '33333333-3333-3333-3333-333333333342'::uuid),
        ('33333333-3333-3333-3333-333333333334'::uuid, 'era:1968', '33333333-3333-3333-3333-333333333343'::uuid),
        ('33333333-3333-3333-3333-333333333335'::uuid, 'dialogue:chi-nam', '33333333-3333-3333-3333-333333333351'::uuid),
        ('33333333-3333-3333-3333-333333333335'::uuid, 'tour:tram-xa', '33333333-3333-3333-3333-333333333352'::uuid),
        ('33333333-3333-3333-3333-333333333335'::uuid, 'era:1968', '33333333-3333-3333-3333-333333333353'::uuid)
),
mapping AS (
    SELECT s.id AS old_id, f.new_id
    FROM quest_steps s
    JOIN fixed f ON f.quest_id = s.quest_id AND f.unlock_key = s.unlock_key
    WHERE s.id <> f.new_id
      AND NOT EXISTS (SELECT 1 FROM quest_steps o WHERE o.id = f.new_id)
),
rebound AS (
    UPDATE discovery_content_bindings b
    SET quest_step_id = m.new_id
    FROM mapping m
    WHERE b.quest_step_id = m.old_id
    RETURNING b.id
)
UPDATE quest_steps s
SET id = m.new_id
FROM mapping m
WHERE s.id = m.old_id;

INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective, description, hint,
                         action_type, action_label, xp_partial, chat_prompt, portal_era, preview_image,
                         minigame_id, is_required, is_premium)
SELECT v.id::uuid, v.quest_id::uuid, v.step_order, v.unlock_key, v.title, v.objective, v.description, v.hint,
       v.action_type, v.action_label, NULL, v.chat_prompt, v.portal_era, v.preview_image,
       NULL, v.is_required, v.is_premium
FROM (VALUES
    -- 333 Hành trình dưới lòng đất
    ('33333333-3333-3333-3333-333333333301', '33333333-3333-3333-3333-333333333333', 1,
     'scene:22222222-2222-2222-2222-222222222221', 'Tour · Bếp Hoàng Cầm',
     'Ở lại cảnh Bếp Hoàng Cầm trong tour 360°.',
     'Một trong hai cảnh tour của hành trình. Đã xem từ trước thì bước được tính luôn.',
     'Mở tour và đứng tại cảnh bếp vài giây.',
     'tour', 'Vào tour', NULL, NULL::int, '/media/cu-chi/scenes/bep-hoang-cam-2026.jpg', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333302', '33333333-3333-3333-3333-333333333333', 2,
     'scene:22222222-2222-2222-2222-222222222223', 'Tour · Phòng họp',
     'Ở lại cảnh phòng họp dưới lòng đất trong tour 360°.',
     'Cảnh tour thứ hai. Xem đủ hai cảnh tour cũng mở các nhiệm vụ Bếp không khói, Nắp hầm và Ba tầng.',
     'Mở tour và đứng tại phòng họp vài giây.',
     'tour', 'Vào tour', NULL, NULL, '/media/cu-chi/scenes/phong-hop-2026.jpg', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333305', '33333333-3333-3333-3333-333333333333', 3,
     'xuanay:bep-hoang-cam', 'Xưa & Nay · Bếp Hoàng Cầm',
     'Tự kéo thanh so sánh Bếp Hoàng Cầm năm 1968 và hôm nay, rồi ở lại ít nhất 4 giây.',
     'Ảnh 1968 là ảnh tái hiện, dựng cùng góc nhìn với ảnh chụp hiện nay.',
     'Mở Cổng thời gian → Xưa & Nay → Bếp Hoàng Cầm.',
     'xuanay', 'Xem Xưa & Nay', NULL, NULL, '/images/xua-nay/bep-hoang-cam-1968.webp', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333303', '33333333-3333-3333-3333-333333333333', 4,
     'era:1948', 'Cổng thời gian · 1948',
     'Xem hết chương 1948 trong Dòng thời gian (tới câu hỏi cuối chương).',
     'Bước tùy chọn dành cho tài khoản Premium, không bắt buộc để hoàn thành nhiệm vụ.',
     'Mở Cổng thời gian và chọn mốc 1948.',
     'portal', 'Mở Cổng thời gian', NULL, 1948, '/media/heritage/dia-dao-cu-chi/cover.jpeg', FALSE, TRUE),
    -- 334 Mật mã Sa bàn Tác chiến
    ('33333333-3333-3333-3333-333333333341', '33333333-3333-3333-3333-333333333334', 1,
     'dialogue:anh-ba', 'Hỏi Chrono về các tầng',
     'Nghe Chrono kể cấu trúc 3 tầng và độ sâu tầng sâu nhất.',
     'Tầng sâu nhất sâu 8–10 m, tối đa 12 m.',
     'Gửi câu hỏi đã soạn. Chrono kể theo tư liệu.',
     'dialogue', 'Hỏi Chrono', 'Chrono ơi, địa đạo Củ Chi có mấy tầng, tầng sâu nhất sâu bao nhiêu?', NULL,
     '/media/characters/nam-du-kich.png', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333342', '33333333-3333-3333-3333-333333333334', 2,
     'tour:sa-ban', 'Mảnh sa bàn',
     'Tìm mảnh giấu gần nhà sa bàn trên sơ đồ, rồi đưa mã cho Chrono.',
     'Đứng trong tour không đủ. Mảnh nằm gần nhà biểu diễn sa bàn.',
     'Tìm mảnh gần nhà sa bàn, dán mã cho Chrono.',
     'tour', 'Tìm mảnh trên sơ đồ', NULL, NULL, '/media/heritage/dia-dao-cu-chi/cover.jpeg', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333343', '33333333-3333-3333-3333-333333333334', 3,
     'era:1968', 'Cổng thời gian · 1968',
     'Xem hết chương 1968 trong Dòng thời gian (tới câu hỏi cuối chương).',
     'Bước tùy chọn dành cho tài khoản Premium, không bắt buộc để hoàn thành nhiệm vụ.',
     'Mở Cổng thời gian và chọn mốc 1968.',
     'portal', 'Mở Cổng thời gian', NULL, 1968, '/media/heritage/dia-dao-cu-chi/cover.jpeg', FALSE, TRUE),
    -- 335 Ánh sáng Đèn Mù U
    ('33333333-3333-3333-3333-333333333351', '33333333-3333-3333-3333-333333333335', 1,
     'dialogue:chi-nam', 'Hỏi Chrono về bếp Hoàng Cầm',
     'Nghe Chrono kể cách Bếp Hoàng Cầm giấu khói.',
     'Khói đi theo rãnh, lọc qua đất ẩm, thoát lên chỉ còn hơi như sương sớm.',
     'Gửi câu hỏi đã soạn cho Chrono.',
     'dialogue', 'Hỏi Chrono', 'Chrono ơi, Bếp Hoàng Cầm dẫn khói theo rãnh và lọc qua đất ẩm như thế nào?', NULL,
     '/media/characters/nu-du-kich.png', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333352', '33333333-3333-3333-3333-333333333335', 2,
     'tour:tram-xa', 'Khu trạm xá trong Nhà sa bàn',
     'Tìm khu trạm xá trong cảnh Nhà sa bàn của tour 360°.',
     'Trạm xá không có cảnh 360° riêng: khu này nằm trong cảnh Nhà sa bàn.',
     'Mở tour, vào cảnh Nhà sa bàn rồi tìm khu trạm xá.',
     'tour', 'Vào tour', NULL, NULL, '/media/heritage/dia-dao-cu-chi/cover.jpeg', TRUE, FALSE),
    ('33333333-3333-3333-3333-333333333353', '33333333-3333-3333-3333-333333333335', 3,
     'era:1968', 'Cổng thời gian · 1968',
     'Xem hết chương 1968 trong Dòng thời gian (tới câu hỏi cuối chương).',
     'Bước tùy chọn dành cho tài khoản Premium, không bắt buộc để hoàn thành nhiệm vụ.',
     'Mở Cổng thời gian và chọn mốc 1968.',
     'portal', 'Mở Cổng thời gian', NULL, 1968, '/media/heritage/dia-dao-cu-chi/cover.jpeg', FALSE, TRUE)
) AS v(id, quest_id, step_order, unlock_key, title, objective, description, hint,
       action_type, action_label, chat_prompt, portal_era, preview_image, is_required, is_premium)
WHERE EXISTS (SELECT 1 FROM quests q WHERE q.id = v.quest_id::uuid)
ON CONFLICT (quest_id, unlock_key) DO UPDATE
SET step_order = EXCLUDED.step_order,
    title = EXCLUDED.title,
    objective = EXCLUDED.objective,
    description = EXCLUDED.description,
    hint = EXCLUDED.hint,
    action_type = EXCLUDED.action_type,
    action_label = EXCLUDED.action_label,
    xp_partial = NULL,
    chat_prompt = EXCLUDED.chat_prompt,
    portal_era = EXCLUDED.portal_era,
    preview_image = EXCLUDED.preview_image,
    is_required = EXCLUDED.is_required,
    is_premium = EXCLUDED.is_premium;

-- 340: 6 trạm QR. Tiêu đề chép từ stations.name tại thời điểm chạy (API v2 luôn đọc tên mới nhất).
INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective, description, hint,
                         action_type, action_label, is_required, is_premium)
SELECT v.id::uuid, '33333333-3333-3333-3333-333333333340'::uuid, v.step_order, 'station:' || v.code,
       'Trạm ' || v.code || COALESCE(' · ' || s.name, ''),
       'Quét mã QR tại trạm ' || v.code || '.',
       'Cần có mặt tại trạm. Mỗi trạm chỉ cần quét một lần.',
       'Mã QR đặt tại trạm trên lộ trình tham quan.',
       'station', 'Quét QR', TRUE, FALSE
FROM (VALUES
    ('33333333-3333-3333-3333-333333340001', 1, 'ST01'),
    ('33333333-3333-3333-3333-333333340002', 2, 'ST02'),
    ('33333333-3333-3333-3333-333333340003', 3, 'ST03'),
    ('33333333-3333-3333-3333-333333340004', 4, 'ST04'),
    ('33333333-3333-3333-3333-333333340005', 5, 'ST05'),
    ('33333333-3333-3333-3333-333333340006', 6, 'ST06')
) AS v(id, step_order, code)
LEFT JOIN stations s ON s.site_code = 'cu-chi' AND s.code = v.code
WHERE EXISTS (SELECT 1 FROM quests q WHERE q.id = '33333333-3333-3333-3333-333333333340')
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

-- ---------------------------------------------------------------------------
-- 4. 9 di tích khác: 2 bước hiện vật, bỏ era:2026 (trước đây bước chỉ có ở FE)
-- ---------------------------------------------------------------------------
INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective, description, hint,
                         action_type, action_label, preview_image, is_required, is_premium)
SELECT v.id::uuid, v.quest_id::uuid, v.step_order, v.unlock_key, v.title, v.objective, v.description, v.hint,
       'artifact', v.action_label, v.preview_image, TRUE, FALSE
FROM (VALUES
    ('44444444-4444-4444-4444-444444440101', '44444444-4444-4444-4444-444444444401', 1, 'artifact:ben-nha-rong-rong-mai',
     'Hai con rồng trên mái', 'Chiêm ngưỡng kiến trúc giao thoa Á Âu trên mái Nhà Rồng.',
     'Lưỡng long chầu nguyệt hiện ra qua ảnh hiện vật số.', 'Mở hiện vật và xem ảnh lớn.',
     'Chiêm ngưỡng hiện vật', '/media/heritage/ben-nha-rong/cover.webp'),
    ('44444444-4444-4444-4444-444444440102', '44444444-4444-4444-4444-444444444401', 2, 'artifact:ben-nha-rong-vali-may',
     'Chiếc vali mây', 'Khám phá hiện vật cuối — vali mây gợi nhớ hành trình 30 năm.',
     'Hoàn tất hành trình bằng hiện vật kết.', NULL,
     'Xem vali mây', '/media/heritage/ben-nha-rong/cover.webp'),
    ('44444444-4444-4444-4444-444444440201', '44444444-4444-4444-4444-444444444402', 1, 'artifact:chua-thien-mu-thap-phuoc-duyen',
     'Tháp Phước Duyên', 'Chiêm ngưỡng tháp 7 tầng — biểu tượng xứ Huế.',
     'Kiến trúc tháp hiện ra qua ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/chua-thien-mu/cover.webp'),
    ('44444444-4444-4444-4444-444444440202', '44444444-4444-4444-4444-444444444402', 2, 'artifact:chua-thien-mu-bia-da',
     'Bia đá trên lưng rùa', 'Khám phá bia Chúa Nguyễn Phúc Chu (1715).',
     'Khóa hành trình sông Hương.', NULL,
     'Xem bia đá', '/media/heritage/chua-thien-mu/cover.webp'),
    ('44444444-4444-4444-4444-444444440301', '44444444-4444-4444-4444-444444444403', 1, 'artifact:co-do-hoa-lu-long-sang',
     'Long Sàng đá', 'Chiêm ngưỡng Long Sàng — bảo vật quốc gia.',
     'Rồng đá uốn lượn trong tủ sưu tập.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/co-do-hoa-lu/cover.jpeg'),
    ('44444444-4444-4444-4444-444444440302', '44444444-4444-4444-4444-444444444403', 2, 'artifact:co-do-hoa-lu-den-le-dai-hanh',
     'Đền Lê Đại Hành', 'Mở hiện vật đền vua đánh tan quân Tống.',
     'Hoàn tất hành trình kinh đô.', NULL,
     'Xem đền thờ', '/media/heritage/co-do-hoa-lu/cover.jpeg'),
    ('44444444-4444-4444-4444-444444440401', '44444444-4444-4444-4444-444444444404', 1, 'artifact:hoang-thanh-thang-long-cot-co',
     'Móng cột cổ', 'Nhìn tầng lịch sử Lý–Trần–Lê qua hiện vật khai quật.',
     'Lớp đất đá dưới 18 Hoàng Diệu trong ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/hoang-thanh-thang-long/cover.webp'),
    ('44444444-4444-4444-4444-444444440402', '44444444-4444-4444-4444-444444444404', 2, 'artifact:hoang-thanh-thang-long-ngoi-phuong',
     'Ngói men xanh', 'Xem gạch ngói hoàng cung thời Lý.',
     'Khóa các lớp di sản Thăng Long.', NULL,
     'Xem ngói hoàng cung', '/media/heritage/hoang-thanh-thang-long/cover.webp'),
    ('44444444-4444-4444-4444-444444440501', '44444444-4444-4444-4444-444444444405', 1, 'artifact:pho-co-hoi-an-chua-cau',
     'Chùa Cầu', 'Chiêm ngưỡng biểu tượng Hội An trên sông Thu Bồn.',
     'Cầu chùa Nhật Bản qua ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/pho-co-hoi-an/cover.webp'),
    ('44444444-4444-4444-4444-444444440502', '44444444-4444-4444-4444-444444444405', 2, 'artifact:pho-co-hoi-an-nha-co-tan-ky',
     'Nhà cổ Tấn Ký', 'Mở hiện vật nhà gỗ hơn 200 năm.',
     'Khóa hành trình thương nhân phố cổ.', NULL,
     'Xem nhà cổ', '/media/heritage/pho-co-hoi-an/cover.webp'),
    ('44444444-4444-4444-4444-444444440601', '44444444-4444-4444-4444-444444444406', 1, 'artifact:thanh-nha-ho-cong-nam',
     'Cổng Nam', 'Nhìn lối vào thành đá thời Hồ.',
     'Kiến trúc quân sự trong tủ sưu tập.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/thanh-nha-ho/cover.webp'),
    ('44444444-4444-4444-4444-444444440602', '44444444-4444-4444-4444-444444444406', 2, 'artifact:thanh-nha-ho-gach-tay-do',
     'Gạch in chữ Tây Đô', 'Chiêm ngưỡng gạch cổ 600 năm.',
     'Hoàn tất hành trình vệ binh Tây Đô.', NULL,
     'Xem gạch cổ', '/media/heritage/thanh-nha-ho/cover.webp'),
    ('44444444-4444-4444-4444-444444440701', '44444444-4444-4444-4444-444444444407', 1, 'artifact:van-mieu-quoc-tu-giam-khue-van-cac',
     'Khuê Văn Các', 'Chiêm ngưỡng biểu tượng văn học Hà Nội.',
     'Gác vẻ đẹp sao Khuê qua ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp'),
    ('44444444-4444-4444-4444-444444440702', '44444444-4444-4444-4444-444444444407', 2, 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
     'Hồ Thiên Quang', 'Mở hiện vật ao cá trong khuôn viên.',
     'Khóa hành trình môn sinh Quốc Tử.', NULL,
     'Xem hồ sen', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp'),
    ('44444444-4444-4444-4444-444444440801', '44444444-4444-4444-4444-444444444408', 1, 'artifact:dai-noi-hue-ngo-mon',
     'Ngọ Môn', 'Chiêm ngưỡng cửa chính Đại Nội.',
     'Quyền lực triều đình qua ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/dai-noi-hue/cover.webp'),
    ('44444444-4444-4444-4444-444444440802', '44444444-4444-4444-4444-444444444408', 2, 'artifact:dai-noi-hue-cuu-dinh',
     'Cửu Đỉnh', 'Khám phá 9 chiếc đỉnh đồng — bảo vật quốc gia.',
     'Hoàn tất hành trình sứ giả triều Nguyễn.', NULL,
     'Xem Cửu Đỉnh', '/media/heritage/dai-noi-hue/cover.webp'),
    ('44444444-4444-4444-4444-444444440901', '44444444-4444-4444-4444-444444444409', 1, 'artifact:den-hung-vuong-trong-dong',
     'Trống đồng Đông Sơn', 'Chiêm ngưỡng hoa văn trống đồng.',
     'Biểu tượng văn hóa Hùng Vương qua ảnh hiện vật.', NULL,
     'Chiêm ngưỡng hiện vật', '/media/heritage/den-hung-vuong/cover.webp'),
    ('44444444-4444-4444-4444-444444440902', '44444444-4444-4444-4444-444444444409', 2, 'artifact:den-hung-vuong-cot-da-the',
     'Cột đá thề', 'Mở hiện vật cột tưởng niệm trên Nghĩa Lĩnh.',
     'Khóa hành trình về Đất Tổ.', NULL,
     'Xem cột đá', '/media/heritage/den-hung-vuong/cover.webp')
) AS v(id, quest_id, step_order, unlock_key, title, objective, description, hint, action_label, preview_image)
WHERE EXISTS (SELECT 1 FROM quests q WHERE q.id = v.quest_id::uuid)
ON CONFLICT (quest_id, unlock_key) DO NOTHING;

DELETE FROM quest_steps
WHERE quest_id IN (SELECT id FROM quests WHERE id::text LIKE '44444444-4444-4444-4444-4444444444%')
  AND unlock_key = 'era:2026';

UPDATE quests q
SET track = 'ONLINE',
    sort_order = 10,
    difficulty = v.difficulty,
    estimated_minutes = 12
FROM (VALUES
    ('44444444-4444-4444-4444-444444444401', 'EASY'),
    ('44444444-4444-4444-4444-444444444402', 'EASY'),
    ('44444444-4444-4444-4444-444444444403', 'MEDIUM'),
    ('44444444-4444-4444-4444-444444444404', 'MEDIUM'),
    ('44444444-4444-4444-4444-444444444405', 'EASY'),
    ('44444444-4444-4444-4444-444444444406', 'MEDIUM'),
    ('44444444-4444-4444-4444-444444444407', 'EASY'),
    ('44444444-4444-4444-4444-444444444408', 'MEDIUM'),
    ('44444444-4444-4444-4444-444444444409', 'EASY')
) AS v(id, difficulty)
WHERE q.id = v.id::uuid;

-- Mô tả cũ nhắc tới bước Time Portal đã bỏ. Chỉ sửa khi văn bản vẫn còn nhắc tới nó.
UPDATE quests q
SET description = v.description,
    story = v.story
FROM (VALUES
    ('44444444-4444-4444-4444-444444444401', 'Chiêm ngưỡng rồng trên mái, rồi khám phá chiếc vali mây.',
     'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Nhìn rồng trên mái, rồi khám phá vali mây.'),
    ('44444444-4444-4444-4444-444444444402', 'Tháp Phước Duyên và bia đá trên lưng rùa.',
     'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chiêm ngưỡng tháp, rồi mở bia đá.'),
    ('44444444-4444-4444-4444-444444444403', 'Long Sàng đá và đền Lê Đại Hành.',
     '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Khám phá Long Sàng, rồi mở đền Lê Đại Hành.'),
    ('44444444-4444-4444-4444-444444444404', 'Móng cột cổ và ngói men xanh.',
     'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp hiện vật.'),
    ('44444444-4444-4444-4444-444444444405', 'Chùa Cầu và nhà cổ Tấn Ký.',
     'Hội An không chỉ đẹp trong ảnh. Nhìn Chùa Cầu, rồi mở nhà Tấn Ký.'),
    ('44444444-4444-4444-4444-444444444406', 'Cổng Nam và gạch in chữ Tây Đô.',
     'Thành không vữa nhưng đứng vững 600 năm. Khám phá cổng Nam, rồi gạch in chữ Tây Đô.'),
    ('44444444-4444-4444-4444-444444444407', 'Khuê Văn Các và Hồ Thiên Quang.',
     'Chiêm ngưỡng Khuê Văn Các, rồi mở Hồ Thiên Quang.'),
    ('44444444-4444-4444-4444-444444444408', 'Ngọ Môn và Cửu Đỉnh.',
     'Chiêm ngưỡng Ngọ Môn, rồi mở Cửu Đỉnh.'),
    ('44444444-4444-4444-4444-444444444409', 'Trống đồng và cột đá thề.',
     'Chiêm ngưỡng trống đồng, rồi mở cột đá thề.')
) AS v(id, description, story)
WHERE q.id = v.id::uuid
  AND (q.description LIKE '%Time Portal%' OR q.story LIKE '%Time Portal%');

-- ---------------------------------------------------------------------------
-- 5. Đồng bộ các cột cũ từ quest_steps (để API cũ và module khác đọc đúng)
-- ---------------------------------------------------------------------------
-- step_discovery_keys = khóa khám phá của bước bắt buộc (không gồm station/checkin);
-- steps_total = số bước; completion_trigger / require_onsite_checkin theo nhóm; required_order thôi dùng.
WITH agg AS (
    SELECT s.quest_id,
           COUNT(*) AS total,
           STRING_AGG(s.unlock_key, ',' ORDER BY s.step_order)
               FILTER (WHERE s.is_required
                         AND COALESCE(s.action_type, '') NOT IN ('station', 'checkin')
                         AND s.unlock_key NOT LIKE 'station:%'
                         AND s.unlock_key NOT LIKE 'checkin:%') AS required_keys
    FROM quest_steps s
    GROUP BY s.quest_id
)
UPDATE quests q
SET steps_total = a.total,
    step_discovery_keys = a.required_keys,
    completion_trigger = CASE WHEN q.track = 'ONSITE' THEN 'checkin' ELSE 'discovery' END,
    require_onsite_checkin = (q.track = 'ONSITE'),
    required_order = NULL
FROM agg a
WHERE q.id = a.quest_id
  AND (q.id::text LIKE '33333333-3333-3333-3333-3333333333%' OR q.id::text LIKE '44444444-4444-4444-4444-4444444444%');

-- END migration: V84__quest_tracks_v2_content.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V85__quest_media_and_minigames_v2.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V85__quest_media_and_minigames_v2.sql
-- ============================================================================
-- Nhiệm vụ v2 (Giai đoạn 3) — ảnh và mini game.
-- Spec: BE/docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md (mục "Giai đoạn 3")
--
-- • Ảnh bìa và ảnh bước của nhiệm vụ Củ Chi: mỗi ảnh khác nhau trong cùng một nhiệm vụ,
--   dùng ảnh thật nằm trong FE/public/images (không phụ thuộc tệp R2 chưa tải lên).
-- • Cấu hình mini game: ảnh thật thay cho SVG giữ chỗ, Đường khói thành lưới rãnh có bếp/hầm khói/miệng thoát,
--   Xếp tầng thêm độ sâu từng tầng, Im lặng thêm dữ kiện có nguồn, Đếm dấu thành đếm lỗ thông hơi.
-- • Id mức chơi giữ nguyên để user_game_results.last_level_id vẫn khớp.
-- Chạy lại an toàn (chỉ UPDATE theo id).

-- ---------------------------------------------------------------------------
-- 1. Ảnh bìa nhiệm vụ Củ Chi
-- ---------------------------------------------------------------------------
UPDATE quests q
SET cover_image = v.cover
FROM (VALUES
    ('33333333-3333-3333-3333-333333333334', '/images/time-portal/era-1968-meeting.jpg'),
    ('33333333-3333-3333-3333-333333333335', '/images/xua-nay/den-ham-nay.webp'),
    ('33333333-3333-3333-3333-333333333336', '/images/time-portal/era-1968-tunnel.jpg'),
    ('33333333-3333-3333-3333-333333333337', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333333338', '/images/cu-chi/nap-ham-mo.jpg'),
    ('33333333-3333-3333-3333-333333333339', '/images/time-portal/era-2026-main.png'),
    ('33333333-3333-3333-3333-333333333340', '/images/cu-chi/rung-dia-dao.jpg')
) AS v(id, cover)
WHERE q.id = v.id::uuid;

-- ---------------------------------------------------------------------------
-- 2. Ảnh từng bước (không trùng nhau, không trùng ảnh bìa trong cùng nhiệm vụ)
-- ---------------------------------------------------------------------------
UPDATE quest_steps s
SET preview_image = v.image
FROM (VALUES
    -- 333 Hành trình dưới lòng đất
    ('33333333-3333-3333-3333-333333333301', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333333302', '/images/xua-nay/phong-hop-nay.webp'),
    ('33333333-3333-3333-3333-333333333305', '/images/xua-nay/bep-hoang-cam-1968.webp'),
    ('33333333-3333-3333-3333-333333333303', '/images/time-portal/era-1948-main.png'),
    -- 334 Mật mã Sa bàn Tác chiến
    ('33333333-3333-3333-3333-333333333341', '/images/characters/anh-ba.png'),
    ('33333333-3333-3333-3333-333333333342', '/images/time-portal/site-diorama.jpg'),
    ('33333333-3333-3333-3333-333333333343', '/images/time-portal/era-1968-main.png'),
    -- 335 Ánh sáng Đèn Mù U
    ('33333333-3333-3333-3333-333333333351', '/images/characters/chi-nam.png'),
    ('33333333-3333-3333-3333-333333333352', '/images/time-portal/era-1968-surgery.jpg'),
    ('33333333-3333-3333-3333-333333333353', '/images/time-portal/era-1968-crawl.jpg'),
    -- 336 Mật lệnh thử game
    ('33333333-3333-3333-3333-333333333361', '/images/cu-chi/so-do-ba-tang.jpg'),
    ('33333333-3333-3333-3333-333333333362', '/images/minigames/soi-nap-cover.jpg'),
    ('33333333-3333-3333-3333-333333333363', '/images/time-portal/era-1975-mid.png'),
    ('33333333-3333-3333-3333-333333333364', '/images/minigames/dem-lo-1.jpg'),
    -- 337 Bếp không khói
    ('33333333-3333-3333-3333-333333333371', '/images/xua-nay/bep-hoang-cam-nay.webp'),
    ('33333333-3333-3333-3333-333333333372', '/images/xua-nay/bep-hoang-cam-1968.webp'),
    -- 338 Nắp hầm và cạm bẫy
    ('33333333-3333-3333-3333-333333333381', '/images/time-portal/site-hatch.jpg'),
    ('33333333-3333-3333-3333-333333333382', '/images/minigames/soi-nap-cover.jpg'),
    ('33333333-3333-3333-3333-333333333383', '/images/cu-chi/du-kich-chien-dau.jpg'),
    -- 339 Ba tầng lòng đất
    ('33333333-3333-3333-3333-333333333391', '/images/cu-chi/duong-ham.jpg'),
    ('33333333-3333-3333-3333-333333333392', '/images/cu-chi/so-do-ba-tang.jpg'),
    -- 340 Theo dấu địa đạo (6 trạm)
    ('33333333-3333-3333-3333-333333340001', '/images/cu-chi/tram-st01-phong-hop.jpg'),
    ('33333333-3333-3333-3333-333333340002', '/images/time-portal/site-kitchen.jpg'),
    ('33333333-3333-3333-3333-333333340003', '/images/cu-chi/tram-st03-gieng.jpg'),
    ('33333333-3333-3333-3333-333333340004', '/images/cu-chi/tram-st04-quan-y.jpg'),
    ('33333333-3333-3333-3333-333333340005', '/images/cu-chi/tram-st05-xuong-vu-khi.jpg'),
    ('33333333-3333-3333-3333-333333340006', '/images/cu-chi/tram-st06-lo-chau-mai.jpg')
) AS v(id, image)
WHERE s.id = v.id::uuid;

-- ---------------------------------------------------------------------------
-- 3. Mini game
-- ---------------------------------------------------------------------------
-- 3a. Soi nắp (Củ Chi + bản trong "Mật lệnh thử game"): ảnh thật nắp hầm ngụy trang dưới lá.
--     Toạ độ là % theo ảnh 960×640 trong FE/public/images/minigames.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://dsvh.gov.vn/di-tich-lich-su-dia-dao-cu-chi-1490",
     "text": "Chỗ lên xuống giữa các tầng có nắp hầm bí mật, cửa lên được ngụy trang."}
  ],
  "levels": [
    {"id": "lid-1",
     "prompt": "Dưới lớp lá có một nắp hầm đang hé mở. Tìm và chạm vào nó trước khi hết 10 giây.",
     "imageUrl": "/images/minigames/soi-nap-1.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 49, "targetY": 36.3, "radius": 10},
    {"id": "lid-2",
     "prompt": "Góc nhìn khác, nắp nằm chỗ khác. Chạm hụt 3 lần là bị lộ.",
     "imageUrl": "/images/minigames/soi-nap-2.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 68, "targetY": 29, "radius": 10},
    {"id": "lid-3",
     "prompt": "Nắp đã khép gần kín, chỉ còn một mép gỗ lộ ra giữa lá khô.",
     "imageUrl": "/images/minigames/soi-nap-3.jpg",
     "revealImageUrl": "/images/minigames/soi-nap-reveal.jpg",
     "timeLimitMs": 10000,
     "targetX": 32, "targetY": 48.3, "radius": 10,
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Khi đóng nắp miệng hầm, kẻ thù đi trên mặt đất khó phát hiện được căn hầm."}}
  ]
}$json$
WHERE id IN ('a1000001-0000-4000-8000-000000000002'::uuid, 'a1000001-0000-4000-8000-000000000012'::uuid);

-- 3b. Đường khói: lưới rãnh nhìn mặt cắt. Ô có "rotation" là đoạn rãnh cần xoay (đáp án, FE không thấy).
--     Hướng: straight 0 = Tây–Đông, 1 = Bắc–Nam; elbow 0 = Bắc–Đông, 1 = Đông–Nam, 2 = Nam–Tây, 3 = Tây–Bắc;
--     tee 0 = Tây–Bắc–Đông, 1 = Bắc–Đông–Nam, 2 = Đông–Nam–Tây, 3 = Nam–Tây–Bắc. "start" là góc ban đầu FE vẽ.
--     Mỗi màn chỉ có một cách nối kín (không rò khói), nên FE tự kiểm tra được và BE chấm lại.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://dantri.com.vn/khoa-hoc/thiet-ke-dac-biet-cua-bep-hoang-cam-de-triet-khoi-che-mat-quan-thu-20250814144359648.htm",
     "text": "Khói đi qua đường rãnh, tản ra, trên mặt đất còn làn khói mỏng bay là là."}
  ],
  "levels": [
    {"id": "dk-1",
     "prompt": "Xoay các đoạn rãnh để khói từ bếp đi qua hầm chứa khói rồi thoát lên mặt đất.",
     "cols": 4, "rows": 3, "parMs": 20000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 2, "open": ["E"]},
       {"id": "a", "kind": "straight", "x": 1, "y": 2, "start": 1, "rotation": 0},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 2, "open": ["W", "E"]},
       {"id": "b", "kind": "elbow", "x": 3, "y": 2, "start": 0, "rotation": 3},
       {"id": "c", "kind": "straight", "x": 3, "y": 1, "start": 0, "rotation": 1},
       {"id": "o1", "kind": "outlet", "x": 3, "y": 0, "open": ["S"]}
     ]},
    {"id": "dk-2",
     "prompt": "Khói phải chia ra hai miệng thoát để chỉ còn làn mỏng trên mặt đất.",
     "cols": 5, "rows": 3, "parMs": 30000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 2, "open": ["E"]},
       {"id": "a", "kind": "elbow", "x": 1, "y": 2, "start": 1, "rotation": 3},
       {"id": "b", "kind": "elbow", "x": 1, "y": 1, "start": 3, "rotation": 1},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 1, "open": ["W", "E"]},
       {"id": "t", "kind": "tee", "x": 3, "y": 1, "start": 2, "rotation": 0},
       {"id": "o1", "kind": "outlet", "x": 3, "y": 0, "open": ["S"]},
       {"id": "d", "kind": "elbow", "x": 4, "y": 1, "start": 1, "rotation": 3},
       {"id": "o2", "kind": "outlet", "x": 4, "y": 0, "open": ["S"]}
     ]},
    {"id": "dk-3",
     "prompt": "Rãnh dài hơn, qua hai hầm khói. Không để khói rò ra chỗ nào.",
     "cols": 5, "rows": 4, "parMs": 45000,
     "cells": [
       {"id": "bep", "kind": "stove", "x": 0, "y": 3, "open": ["E"]},
       {"id": "a", "kind": "straight", "x": 1, "y": 3, "start": 1, "rotation": 0},
       {"id": "b", "kind": "elbow", "x": 2, "y": 3, "start": 0, "rotation": 3},
       {"id": "h1", "kind": "chamber", "x": 2, "y": 2, "open": ["S", "N"]},
       {"id": "t", "kind": "tee", "x": 2, "y": 1, "start": 0, "rotation": 2},
       {"id": "c", "kind": "elbow", "x": 1, "y": 1, "start": 2, "rotation": 0},
       {"id": "o1", "kind": "outlet", "x": 1, "y": 0, "open": ["S"]},
       {"id": "d", "kind": "straight", "x": 3, "y": 1, "start": 1, "rotation": 0},
       {"id": "h2", "kind": "chamber", "x": 4, "y": 1, "open": ["W", "N"]},
       {"id": "o2", "kind": "outlet", "x": 4, "y": 0, "open": ["S"]}
     ]}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000011'::uuid;

-- 3c. Xếp tầng: thêm độ sâu từng tầng (baolaocai.vn: ~3 m, ~5 m, ~8–10 m, có đoạn tới 12 m).
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 1 sâu khoảng 3 m, tầng 2 khoảng 5 m, tầng 3 khoảng 8–10 m, có đoạn tới 12 m. Càng sâu càng chịu được bom đạn lớn."},
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 2 chống được bom cỡ nhỏ."},
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Tầng 3 chống được hầu hết các loại bom đạn."}
  ],
  "levels": [
    {"id": "fn-1",
     "prompt": "Đặt mỗi thẻ vào đúng tầng địa đạo. Gợi ý: càng sâu càng chịu được bom đạn lớn.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
       {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"},
       {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"}
     ]},
    {"id": "fn-2",
     "prompt": "Thứ tự thẻ đã đổi. Đặt lại cho đúng tầng.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "most", "label": "Chống được hầu hết bom đạn", "floorId": "t3"},
       {"id": "tank", "label": "Chịu được đạn pháo và sức nặng xe tăng", "floorId": "t1"},
       {"id": "small", "label": "Chống được bom cỡ nhỏ", "floorId": "t2"}
     ],
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Tầng 2 chống được bom cỡ nhỏ."}},
    {"id": "fn-3",
     "prompt": "Lượt cuối, thẻ ngắn hơn. Đừng để sai quá nhiều.",
     "floors": [
       {"id": "t1", "label": "Tầng 1", "depth": "sâu khoảng 3 m"},
       {"id": "t2", "label": "Tầng 2", "depth": "sâu khoảng 5 m"},
       {"id": "t3", "label": "Tầng 3", "depth": "sâu khoảng 8–10 m"}
     ],
     "items": [
       {"id": "small", "label": "Bom cỡ nhỏ", "floorId": "t2"},
       {"id": "most", "label": "Hầu hết bom đạn", "floorId": "t3"},
       {"id": "tank", "label": "Đạn pháo và xe tăng", "floorId": "t1"}
     ],
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Tầng 3 chống được hầu hết các loại bom đạn."}}
  ]
}$json$
WHERE id IN ('a1000001-0000-4000-8000-000000000001'::uuid, 'a1000001-0000-4000-8000-000000000013'::uuid);

-- 3d. Đào gặp nhau: giữ đáp án, thêm khoảng cách hai giếng để FE vẽ (dữ kiện: 6–15 m).
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baophapluat.vn/doi-quan-xuat-quy-nhap-than-bai-3-thien-la-dia-vong-cho-quan-my-duoi-long-dat-cu-chi-post293888.html",
     "text": "Hai giếng cách nhau khoảng 6–15 m, đào xuống khoảng 3–4 m rồi hai người đào hướng vào nhau."}
  ],
  "levels": [
    {"id": "well-1", "prompt": "Chỉnh hướng đào để gặp đồng đội đang đào từ giếng bên kia.",
     "distanceM": 8, "angleMin": -60, "angleMax": 60, "targetAngle": -28, "tolerance": 8},
    {"id": "well-2", "prompt": "Giếng này lệch hướng khác. Lắng nghe tiếng cuốc để chỉnh.",
     "distanceM": 11, "angleMin": -60, "angleMax": 60, "targetAngle": 16, "tolerance": 8},
    {"id": "well-3", "prompt": "Hai giếng xa nhau hơn. Chỉ có 6 lần đào.",
     "distanceM": 14, "angleMin": -60, "angleMax": 60, "targetAngle": 44, "tolerance": 8}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000003'::uuid;

-- 3e. Đếm dấu → đếm lỗ thông hơi ngụy trang trên nền lá (ảnh trong FE/public/images/minigames).
UPDATE quest_minigames
SET title = 'Đếm lỗ thông hơi',
    config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Dọc đường hầm có các lỗ thông hơi được ngụy trang kín đáo, đưa lên mặt đất qua nhiều cửa bí mật."}
  ],
  "levels": [
    {"id": "dem-1", "prompt": "Có bao nhiêu lỗ thông hơi lẫn trong lớp lá? Chạm để đánh dấu từng lỗ rồi chốt số.",
     "imageUrl": "/images/minigames/dem-lo-1.jpg", "answerCount": 4},
    {"id": "dem-2", "prompt": "Khu rừng khác, nhiều lỗ hơn. Đếm kỹ trước khi chốt.",
     "imageUrl": "/images/minigames/dem-lo-2.jpg", "answerCount": 7},
    {"id": "dem-3", "prompt": "Lượt cuối. Có lỗ bị lá che một phần.",
     "imageUrl": "/images/minigames/dem-lo-3.jpg", "answerCount": 5}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000004'::uuid;

UPDATE quest_steps
SET title = 'Chương 4 · Đếm lỗ thông hơi'
WHERE id = '33333333-3333-3333-3333-333333333364'::uuid;

-- 3f. Im lặng: thêm dữ kiện có nguồn, lời dẫn theo tình huống lính tuần tra trên đầu.
UPDATE quest_minigames
SET config = $json${
  "facts": [
    {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
     "text": "Khi đóng nắp miệng hầm, kẻ thù đi trên mặt đất khó phát hiện được căn hầm."}
  ],
  "levels": [
    {"id": "im-1", "holdMs": 3000, "marginRatio": 1.8,
     "prompt": "Lính tuần tra đang đi ngang trên đầu. Giữ im lặng (hoặc giữ nút) cho tới khi tiếng bước chân đi qua."},
    {"id": "im-2", "holdMs": 3000, "marginRatio": 1.5,
     "prompt": "Toán lính dừng lại gần nắp hầm. Lần này phải im hơn nữa.",
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Cán bộ sống trong vùng địch ban ngày náu mình dưới hầm bí mật, ban đêm mới lên khỏi mặt đất."}},
    {"id": "im-3", "holdMs": 4000, "marginRatio": 1.5,
     "prompt": "Cuộc lùng sục kéo dài hơn. Giữ im lâu hơn.",
     "fact": {"url": "https://baolaocai.vn/dia-dao-cu-chi-mot-huyen-thoai-cua-viet-nam-trong-the-ky-20-post399825.html",
              "text": "Cán bộ sống trong vùng địch ban ngày náu mình dưới hầm bí mật, ban đêm mới lên khỏi mặt đất."}}
  ]
}$json$
WHERE id = 'a1000001-0000-4000-8000-000000000014'::uuid;

-- ---------------------------------------------------------------------------
-- 4. Lời giới thiệu trạm hiện cho người dùng: bỏ ghi chú nội bộ còn sót trong seed.
-- ---------------------------------------------------------------------------
UPDATE station_content_blocks
SET body = btrim(replace(replace(body,
        ' Nội dung chi tiết sẽ được BQL xác thực sau khảo sát Tuần 0.', ''),
        ' (nếu nằm trên tuyến tham quan)', ''))
WHERE body LIKE '%BQL xác thực sau khảo sát Tuần 0%'
   OR body LIKE '%(nếu nằm trên tuyến tham quan)%';

UPDATE story_chapters
SET synopsis = replace(synopsis, ' (nếu trên tuyến)', '')
WHERE synopsis LIKE '%(nếu trên tuyến)%';

-- END migration: V85__quest_media_and_minigames_v2.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V86__cu_chi_quest_333_cover.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V86__cu_chi_quest_333_cover.sql
-- ============================================================================
-- Ảnh bìa nhiệm vụ 333 "Hành trình dưới lòng đất" là tệp duy nhất của Củ Chi còn nằm trên R2
-- (/media/heritage/...), nên thẻ nhiệm vụ hiện ô xám khi R2 chưa sẵn. Đổi sang ảnh trong FE/public
-- cho đồng bộ với 7 nhiệm vụ còn lại (V85). Ảnh này không trùng ảnh bước nào của nhiệm vụ.
-- Chạy lại an toàn.

UPDATE quests
SET cover_image = '/images/cu-chi/loi-xuong-ham.jpg'
WHERE id = '33333333-3333-3333-3333-333333333333'::uuid;

-- END migration: V86__cu_chi_quest_333_cover.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V87__passport_mail_attempts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V87__passport_mail_attempts.sql
-- ============================================================================
-- Reconcile both a new database and a table created earlier by Hibernate ddl-auto=update.
-- Fail on incompatible/orphaned existing data; never silently remove user history.
CREATE TABLE IF NOT EXISTS passport_mail_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    request_id UUID NOT NULL,
    image_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    state VARCHAR(16) NOT NULL
);

ALTER TABLE passport_mail_attempts
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN request_id SET NOT NULL,
    ALTER COLUMN image_sha256 SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN state SET NOT NULL;

DO $passport_mail$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'fk_passport_mail_profile'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT fk_passport_mail_profile
            FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'ck_passport_mail_image_hash'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT ck_passport_mail_image_hash
            CHECK (image_sha256 ~ '^[0-9a-f]{64}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'ck_passport_mail_state'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT ck_passport_mail_state
            CHECK (state IN ('CREATED','ACCEPTED','UNKNOWN'));
    END IF;

    -- Hibernate may already have generated the same unique constraint under a different name.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND contype IN ('u','p')
          AND conkey = ARRAY[
              (SELECT attnum FROM pg_attribute WHERE attrelid = 'passport_mail_attempts'::regclass AND attname = 'user_id'),
              (SELECT attnum FROM pg_attribute WHERE attrelid = 'passport_mail_attempts'::regclass AND attname = 'request_id')
          ]::smallint[]
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT uq_passport_mail_user_request UNIQUE (user_id, request_id);
    END IF;
END;
$passport_mail$;

CREATE INDEX IF NOT EXISTS idx_passport_mail_user_created
    ON passport_mail_attempts(user_id, created_at DESC);

-- END migration: V87__passport_mail_attempts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V88__repair_vietnamese_content.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V88__repair_vietnamese_content.sql
-- ============================================================================
-- Repair data that was irreversibly stored with Vietnamese characters replaced by literal '?'.
-- Replaying the canonical UTF-8 dataset restores all 10 location descriptions and their artifacts.
-- The statements are idempotent via their existing ON CONFLICT clauses.

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222201', 'Bến Nhà Rồng', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 01 tòa nhà Nguyễn Tất Thành, Phường 12, Quận 4, TP. Hồ Chí Minh, ngay ngã ba sông Sài Gòn và kênh Bến Nghé.
Tên gọi chính thức hiện nay: Bảo tàng Hồ Chí Minh – Chi nhánh TP. Hồ Chí Minh.
Tầm vóc: Đây là một cụm di tích kiến trúc - lịch sử đặc biệt. Nơi đây không chỉ lưu giữ cấu trúc thương cảng kiểu Pháp bề thế mà còn là "địa chỉ đỏ" gìn giữ hơn 20.000 tài liệu, hiện vật về cuộc đời và sự nghiệp cách mạng của Bác Hồ.
2. Lịch sử hình thành: Từ trụ sở thương cảng đến chứng nhân lịch sử
Xây dựng (1862 - 1863): Sau khi chiếm được Gia Định, thực dân Pháp cho xây dựng tòa nhà này để làm trụ sở cho Hãng vận tải đường biển Hoàng gia Pháp (Messageries Maritimes). Đây là một trong những công trình kiến trúc phương Tây đầu tiên được xây dựng tại Sài Gòn.
Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:
"Tái hiện con tàu lịch sử" bằng AR:
Khi đứng tại khu vực bến cảng hướng ra sông Sài Gòn, du khách có thể sử dụng điện thoại thông minh quét mã AR để "triệu hồi" hình ảnh 3D của con tàu buôn cổ Amiral Latouche-Tréville đang neo đậu ngay trên bến sông thật.
Ứng dụng AR sẽ mô phỏng lại khoảnh khắc người thanh niên Văn Ba bước lên mạn tàu, chào tạm biệt bến cảng Sài Gòn trong không gian chuyển động 3D, giúp người xem cảm nhận rõ nét bối cảnh lịch sử của ngày 5/6 năm ấy.
Số hóa bản đồ "Hành trình 30 năm vạn dặm": Bảo tàng trang bị hệ thống màn hình cảm ứng tương tác cỡ lớn, số hóa toàn bộ bản đồ thế giới ghi dấu những nơi Bác đã đi qua. Du khách chỉ cần chạm vào các quốc gia như Pháp, Anh, Nga... bản đồ AR sẽ hiển thị các tư liệu ảnh, bài báo, hiện vật liên quan đến hoạt động của Bác tại quốc gia đó kèm giọng thuyết minh tự động.
Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 10.768, 106.707, 'TP.HCM', '/media/heritage/ben-nha-rong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Bến Nhà Rồng
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000001-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Hai con rồng trên mái Nhà Rồng', '/media/heritage/ben-nha-rong/cover.webp', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc…', 'Nguồn gốc tên gọi "Nhà Rồng": Tòa nhà được xây theo kiến trúc phương Tây nhưng trên đỉnh mái lại được gắn hai con rồng lớn bằng đất nung tráng men chầu mặt trăng theo mô-típ "Lưỡng long chầu nguyệt" truyền thống của kiến trúc đình chùa Việt Nam. Sự kết hợp độc đáo này khiến người dân Sài Gòn lúc bấy giờ quen gọi tòa nhà là "Nhà Rồng" và bến cảng xung quanh là "Bến Nhà Rồng".
Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-rong-mai', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000002-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Con tàu Amiral Latouche-Tréville', '/media/heritage/ben-nha-rong/cover.webp', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm…', 'Sự kiện lịch sử vĩ đại (05/06/1911): Trên con tàu buôn Amiral Latouche-Tréville của Pháp đang neo đậu tại Bến Nhà Rồng, người thanh niên Nguyễn Tất Thành với hai bàn tay trắng và tên gọi mới Văn Ba đã chính thức rời Tổ quốc, bắt đầu chuyến hành trình kéo dài 30 năm qua khắp các châu lục để tìm ra con đường giải phóng dân tộc.
3. Kiến trúc giao thoa & Các khu vực trưng bày
Kiến trúc: Tòa nhà Nhà Rồng mang phong cách kiến trúc thuộc địa Pháp với hành lang rộng, các vòm cửa cuốn vững chãi và hệ thống cột trụ uy nghiêm. Tuy nhiên, yếu tố văn hóa Việt (hình tượng rồng trên mái) đã tạo nên nét chấm phá rất riêng, hài hòa giữa hai nền văn hóa Á - Âu.
Không gian bảo tàng: Hiện nay, bảo tàng có nhiều phòng trưng bày cố định và chuyên đề, tái hiện toàn bộ hành trình từ thuở niên thiếu của Bác tại quê nhà Nghệ An, giai đoạn bôn ba tìm đường cứu nước tại Pháp, Anh, Mỹ, Liên Xô, Trung Quốc... cho đến ngày Ngài đọc bản Tuyên ngôn Độc lập và lãnh đạo đất nước.
4. Ứng dụng AR và Chuyển đổi số tương tác tại Bến Nhà Rồng
Để biến những tài liệu giấy và hiện vật đứng yên trở nên sinh động, giàu cảm xúc đối với khách tham quan, Bến Nhà Rồng đã áp dụng nhiều giải pháp công nghệ hiện đại:', 'artifact:ben-nha-rong-tau-amiral', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000003-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Chiếc vali mây của Bác', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-vali-may', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000004-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222201', 'Đôi dép cao su', '/media/heritage/ben-nha-rong/cover.webp', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được…', 'Tour tham quan ảo 360° kết hợp tư liệu số: Hệ thống lưu trữ trực tuyến cho phép những người không có điều kiện đến trực tiếp bảo tàng vẫn có thể đi xuyên qua các phòng trưng bày bằng công nghệ Panorama 360 độ. Các hiện vật như chiếc vali mây, đôi dép cao su, hay những bản thảo viết tay đều được quét 3D sắc nét, cho phép người xem phóng to để đọc từng dòng chữ lịch sử.
Bến Nhà Rồng là nơi bắt đầu của một chương lịch sử mới cho dân tộc Việt Nam. Công nghệ AR và số hóa tại đây giống như một cuốn sách lịch sử mở, giúp thế hệ trẻ dễ dàng thấu hiểu hơn về lòng yêu nước, ý chí sắt đá và tầm nhìn vĩ đại của vị lãnh tụ kính yêu ngay tại nơi Người đã cất bước ra đi.', 'artifact:ben-nha-rong-dep-cao-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222202', 'Chùa thiên mụ', '1. Tổng quan & Vị trí
Vị trí: Chùa nằm trên đồi Hà Khê, thuộc tả ngạn Sông Hương, cách trung tâm thành phố Huế khoảng 5km về phía Tây.
Kiến trúc cảnh quan: Ngôi chùa soi bóng xuống dòng sông Hương thơ mộng, tạo nên một bức tranh sơn thủy hữu tình, mang đậm nét u tịch, trang nghiêm và cổ kính.
2. Lịch sử hình thành và những truyền thuyết
Năm thành lập: Chùa được chính thức khởi công xây dựng vào năm 1601 (tức năm Tân Sửu) bởi vị chúa Nguyễn đầu tiên – Chúa Tiên Nguyễn Hoàng.
Truyền thuyết về tên gọi "Thiên Mụ":
Theo đại nam nhất thống chí, khi chúa Nguyễn Hoàng vào trấn thủ xứ Thuận Hóa, ông đã đích thân đi dọc bờ sông Hương để xem xét địa hình nhằm chuẩn bị cho việc mở mang cơ nghiệp. Khi đến đồi Hà Khê, ông nghe người dân địa phương kể lại rằng: Đêm đêm thường có một bà lão mặc áo đỏ quần lục xuất hiện trên đồi, nói với mọi người rằng "Rồi đây sẽ có một vị chúa đến lập chùa để tụ khí cho bền long mạch, làm cho nước Nam hùng mạnh".
Nói xong, bà lão biến mất vào đám mây. Cho rằng đó là điềm lành từ trời, Nguyễn Hoàng đã cho dựng chùa trên đồi và đặt tên là Thiên Mụ Tự (Chùa Bà Mụ Trời).
Các mốc thời gian quan trọng:
Năm 1665: Chúa Nguyễn Phúc Tần cho trùng tu, mở rộng quy mô chùa.
Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:
Tháp Phước Duyên:
Xây dựng năm 1844 dưới thời vua Thiệu Trị, ban đầu có tên là tháp Từ Nhân.
Tháp cao 7 tầng (khoảng 21m), hình bát giác, mỗi tầng thờ một Đức Phật khác nhau. Đây là một trong những ngọn tháp cổ mang tính biểu tượng cao nhất tại Việt Nam.
Đại Hồng Chung (Chuông đồng):
Được chúa Nguyễn Phúc Chu cho đúc vào năm 1710.
Chuông cao 2,5m, đường kính miệng chuông 1,4m và nặng tới 3.285 cân (khoảng hơn 2 tấn). Tiếng chuông Thiên Mụ từ lâu đã đi vào ca dao, văn học vì độ vang xa và trầm ấm âm vang khắp vùng kinh thành xưa.
Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.
Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.
Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 16.453, 107.558, 'Huế', '/media/heritage/chua-thien-mu/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Chùa thiên mụ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000005-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Tháp Phước Duyên', '/media/heritage/chua-thien-mu/cover.webp', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó…', 'Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-thap-phuoc-duyen', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000006-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Đại Hồng Chung', '/media/heritage/chua-thien-mu/cover.webp', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các…', 'Năm 1710: Chúa Nguyễn Phúc Chu cho đúc chiếc Đại Hồng Chung lớn.
Năm 1714: Chúa Nguyễn Phúc Chu tiếp tục đại trùng tu, xây dựng thêm hàng loạt công trình điện thờ, nhà tăng, lầu bia, và biến Thiên Mụ trở thành ngôi chùa tráng lệ nhất xứ Đàng Trong thời bấy giờ.
Thời nhà Nguyễn (thế kỷ XIX): Các vua Gia Long, Minh Mạng, Thiệu Trị, Tự Đức đều nhiều lần cho trùng tu chùa. Vua Thiệu Trị đã cho xây dựng tháp Phước Duyên (1844) - biểu tượng ngày nay của chùa.
Năm 1904: Một trận bão lịch sử (năm Thìn) đã tàn phá nặng nề nhiều công trình trong chùa, sau đó được vua Thành Thái cho sửa chữa lại nhưng quy mô không còn được như trước.
3. Kiến trúc và Cổ vật có giá trị lịch sử
Chùa Thiên Mụ không chỉ là trung tâm tu học mà còn là một "bảo tàng" ngoài trời lưu giữ nhiều cổ vật quý giá qua các triều đại:', 'artifact:chua-thien-mu-dai-hong-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000007-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Bia đá thời Chúa Nguyễn Phúc Chu', '/media/heritage/chua-thien-mu/cover.webp', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'Bia đá thời Chúa Nguyễn Phúc Chu (1715):
Đặt trên lưng một con rùa đá lớn bằng cẩm thạch. Bia cao hơn 2,5m, khắc bài văn do chính chúa Nguyễn Phúc Chu soạn, ghi lại việc xây dựng và trùng tu chùa, thể hiện tư tưởng Phật giáo thời bấy giờ.', 'artifact:chua-thien-mu-bia-da', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000008-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Điện Đại Hùng', '/media/heritage/chua-thien-mu/cover.webp', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'Điện Đại Hùng:
Là ngôi chính điện của chùa, kiến trúc theo lối trùng thiềm điệp ốc đặc trưng của Huế. Bên trong thờ tượng Phật Tam Thế, tượng Di Lặc và các vị Phật, Bồ Tát khác. Khung cửa và các hệ thống cột kèo gỗ đều được chạm khắc cực kỳ tinh xảo.', 'artifact:chua-thien-mu-dien-dai-hung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000009-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222202', 'Xe Austin Thích Quảng Đức', '/media/heritage/chua-thien-mu/cover.webp', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản…', 'Chiếc xe ô tô di vật của Hòa thượng Thích Quảng Đức:
Nằm ở khu vực phía sau khuôn viên chùa, nơi đây trưng bày chiếc xe ô tô Austin màu xanh xám. Đây chính là chiếc xe đã đưa Bồ tát Thích Quảng Đức từ Huế vào Sài Gòn trước khi ngài thực hiện cuộc tự thiêu vị pháp thiêu thân vào năm 1963 để phản đối chính sách đàn áp Phật giáo của chế độ Ngô Đình Diệm.
4. Giá trị tâm linh và văn hóa
Chùa Thiên Mụ nằm trong danh sách các điểm di tích thuộc Quần thể di tích Cố đô Huế được UNESCO công nhận là Di sản Văn hóa Thế giới năm 1993.
Ngôi chùa gắn liền với đời sống tâm linh của người dân xứ Huế, là nguồn cảm hứng bất tận cho thi ca, nhạc họa qua nhiều thế hệ nhờ vẻ đẹp tĩnh lặng, thanh tịnh giữa không gian trầm mặc của vùng đất Thần Kinh.', 'artifact:chua-thien-mu-xe-austin', 'primary', 5)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222203', 'Cố đô Hoa Lư', '1. Tổng quan & Vị trí
Vị trí: Thuộc xã Trường Yên, huyện Hoa Lư, tỉnh Ninh Bình, cách thủ đô Hà Nội khoảng 90km về phía Nam.
Tầm vóc: Hoa Lư là một trong ba vùng lõi cấu thành nên Quần thể danh thắng Tràng An – Di sản Văn hóa và Thiên nhiên thế giới (di sản hỗn hợp duy nhất tại Đông Nam Á) được UNESCO công nhận vào năm 2014.
Địa thế phong thủy: Kinh đô được bao bọc bởi những dãy núi đá vôi trùng điệp, tạo thành một bức tường thành thiên nhiên kiên cố, xen kẽ giữa các thung lũng là hệ thống sông ngòi chằng chịt, mang tính chất một thành trì quân sự hiểm yếu "bất khả xâm phạm".
2. Lịch sử hình thành: Kinh đô của ba triều đại
Cố đô Hoa Lư đóng vai trò là thủ đô của nước Đại Cồ Việt trong suốt 42 năm (968 – 1010), gắn liền với sự nghiệp của ba triều đại liên tiếp:
Nhà Đinh (968 - 980): Sau khi dùng mưu lược và võ công dẹp loạn 12 sứ quân, thống nhất giang sơn, Đinh Bộ Lĩnh lên ngôi hoàng đế (Đinh Tiên Hoàng), đặt tên nước là Đại Cồ Việt và chọn Hoa Lư làm đất định đô. Ông cho đắp thành, đào hào, xây dựng cung điện.
Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:
Đền vua Đinh Tiên Hoàng:
Nằm ở trung tâm thành Đông, xây dựng theo kiểu "Nội công ngoại quốc" đặc trưng.
Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.
Đền vua Lê Đại Hành:
Cách đền vua Đinh khoảng 300m, quy mô nhỏ hơn một chút nhưng nghệ thuật điêu khắc gỗ và đá vô cùng đặc sắc.
Nơi đây còn lưu giữ những mảng chạm khắc rồng, mây mang đậm phong cách nghệ thuật thời hậu Lê. Đặc biệt là bức tượng vua Lê Đại Hành, thái hậu Dương Vân Nga và kiệu rước cổ.
Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.
Khu khai quật khảo cổ học lòng đất:
Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 20.283, 105.917, 'Ninh Bình', '/media/heritage/co-do-hoa-lu/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Cố đô Hoa Lư
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000010-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Long Sàng đá', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'Long Sàng (Sập đá): Đặt trước Nghi môn ngoại và Thượng điện, được tạc bằng đá nguyên khối từ thế kỷ XVII. Long sàng chạm khắc hình rồng uốn lượn cực kỳ uyển chuyển, tinh xảo, được công nhận là Bảo vật Quốc gia.', 'artifact:co-do-hoa-lu-long-sang', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000011-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Cột kinh Chùa Nhất Trụ', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'Chùa Nhất Trụ (Chùa Một Cột của Hoa Lư):
Ngôi chùa cổ do vua Lê Đại Hành xây dựng. Tại đây có Cột kinh Phật bằng đá khắc bài kinh Thủ Lăng Nghiêm, dựng từ năm 995. Đây là bảo vật quốc gia mang giá trị vô song về lịch sử Phật giáo và nghệ thuật thư pháp đá thế kỷ X.', 'artifact:co-do-hoa-lu-cot-kinh', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000012-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Đền vua Lê Đại Hành', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái…', 'Nhà Tiền Lê (980 - 1009): Vua Lê Hoàn (Lê Đại Hành) lên ngôi, tiếp tục đóng đô tại đây. Ông đã lãnh đạo quân dân đánh tan quân tống xâm lược (981) và mở mang, xây dựng thêm nhiều điện đài lộng lẫy (như điện Bách Bảo Thiên Tuế được dát vàng bạc).
Nhà Lý (Giai đoạn đầu, 1009 - 1010): Vua Lý Thái Tổ (Lý Công Uẩn) lên ngôi tại chính điện Hoa Lư. Nhận thấy vùng đất này tuy hiểm trở nhưng chật hẹp, khó mở mang phát triển lâu dài, mùa thu năm 1010, ông đã viết Chiếu dời đô để chuyển kinh thủ đô về Thăng Long (Hà Nội). Từ đó, Hoa Lư trở thành Cố đô (kinh đô cũ).
3. Di tích kiến trúc & Những báu vật lịch sử
Trải qua hơn 1000 năm với những thăng trầm và tàn phá của thời gian, các cung điện vàng son xưa kia không còn nguyên vẹn trên mặt đất. Tuy nhiên, không gian tâm linh cốt lõi vẫn được lưu giữ qua các công trình đền thờ tôn nghiêm:', 'artifact:co-do-hoa-lu-den-le-dai-hanh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000013-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222203', 'Tiền cổ Đinh Vạn Tuế', '/media/heritage/co-do-hoa-lu/cover.jpeg', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung…', 'Nằm ngay cạnh đền vua Lê, các nhà khảo cổ đã làm xuất lộ hệ thống nền móng kiến trúc cung điện thế kỷ X, dấu tích ngói lợp, gạch lát nền có chạm khắc hình hoa sen, chim phượng và các loại tiền cổ "Đinh Vạn Tuế Thông Bảo".
4. Ứng dụng AR và Số hóa "Hồi sinh" Cố đô vàng son
Chính vì kiến trúc cung điện gốc bằng gỗ của thế kỷ X đã lùi sâu vào lòng đất, công nghệ AR và số hóa là "chìa khóa vàng" giúp du khách tìm lại diện mạo hào hùng của Hoa Lư:
Phục dựng điện Bách Bảo Thiên Tuế bằng AR: Khi đứng tại sân đền hoặc khu khảo cổ, du khách có thể sử dụng điện thoại thông minh quét các điểm tương tác để nhìn thấy toàn bộ hình ảnh cung điện mô phỏng 3D hiện lên trực quan. Bạn sẽ thấy những mái cung điện lợp ngói uy nghiêm, các cột gỗ sơn son, tái hiện đúng mô tả của lịch sử về một kinh thành kiên cố giữa lòng thung lũng đá vôi.
Mô phỏng 3D Sa bàn Kinh đô: Công nghệ thực tế ảo cho phép người xem đứng từ trên cao (góc nhìn chim bay) để quan sát toàn bộ hệ thống Thành Ngoại, Thành Trong, Thành Nam phối hợp với các ngọn núi đá (núi Mã Yên, núi Cột Cờ) tạo thành chiến lũy phòng thủ tự nhiên ra sao.
Số hóa tương tác Bảo vật Quốc gia: Du khách có thể xoay 360 độ và phóng to từng chi tiết chạm khắc trên Long Sàng đá hay Cột kinh Phật Chùa Nhất Trụ để chiêm ngưỡng kỹ nghệ điêu khắc đá điêu luyện của cha ông mà không làm tổn hại đến hiện vật gốc.
Cố đô Hoa Lư là nơi ngưng đọng hào khí tự cường của dân tộc Việt. Dù kiến trúc vật chất đã mai một, nhưng dòng chảy lịch sử và sự trợ lực của công nghệ AR đang giúp thế hệ hôm nay chạm vào quá khứ một cách chân thực và tự hào nhất.', 'artifact:co-do-hoa-lu-tien-co', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222204', 'Hoàng Thành Thăng Long', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 19C Hoàng Diệu, quận Ba Đình, Hà Nội. Khu di tích có diện tích cực kỳ rộng lớn, bao quanh bởi các con đường lịch sử như Hoàng Diệu, Điện Biên Phủ, Phan Đình Phùng và Nguyễn Tri Phương.
Tầm vóc: Đây là quần thể di tích gắn liền với lịch sử kinh thành Thăng Long - Đông Kinh và tỉnh thành Hà Nội. Đây là minh chứng sống động cho sự phát triển liên tục của trung tâm quyền lực chính trị Việt Nam suốt 13 thế kỷ (từ thời tiền Thăng Long thế kỷ VII, qua các triều đại Lý, Trần, Lê, Mạc, Nguyễn đến thời đại Hồ Chí Minh). Khu trung tâm Hoàng thành Thăng Long được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 2010.
2. Lịch sử hình thành qua các tầng đất khảo cổ
Điểm đặc biệt nhất của Hoàng Thành Thăng Long chính là "lịch sử chồng chồng lên nhau" ngay trong lòng đất:
Thời kỳ tiền Thăng Long (Thế kỷ VII - IX): Nơi đây từng là trụ sở của An Nam đô hộ phủ thời nhà Đường (thành Đại La).
Định đô Thăng Long (Năm 1010): Vua Lý Thái Tổ dời đô từ Hoa Lư về Đại La, đổi tên thành Thăng Long và cho xây dựng Hoàng Thành trên nền thành cũ.
Các triều đại tiếp nối (Trần, Lê, Mạc): Tiếp tục mở rộng, xây dựng thêm nhiều cung điện nguy nga. Nơi đây là trung tâm đầu não điều hành đất nước, kinh qua các cuộc chiến chống ngoại xâm hiển hách.
Thời nhà Nguyễn (Thế kỷ XIX): Khi triều Nguyễn chuyển kinh đô vào Phú Xuân (Huế), nơi đây trở thành thành Hà Nội. Vua Gia Long cho phá dỡ một số công trình cũ để xây lại thành theo kiến trúc Vauban của Pháp.
Thời kỳ hiện đại: Trong kháng chiến chống Mỹ, Tổng hành dinh Quân đội Nhân dân Việt Nam (Bộ Chính trị và Quân ủy Trung ương) đã chọn nơi đây làm trung tâm chỉ huy đầu não nhờ hệ thống hầm ngầm kiên cố.
3. Các công trình tiêu biểu & Cổ vật nghìn năm
Khu di tích được chia làm hai khu vực chính: Khu di tích lịch sử Thành cổ Hà Nội và Khu khảo cổ học 18 Hoàng Diệu.
A. Các công trình lộ thiên nổi bật
Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.
Cổ vật hoàng cung:
Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 21.036, 105.817, 'Hà Nội', '/media/heritage/hoang-thanh-thang-long/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Hoàng Thành Thăng Long
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000014-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Cột cờ Hà Nội (Kỳ Đài)', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời…', 'Kỳ Đài (Cột cờ Hà Nội): Xây dựng năm 1812 dưới thời vua Gia Long, cao gần 33m. Đây là công trình hiếm hoi còn nguyên vẹn, sống sót qua thời kỳ thuộc địa Pháp và chiến tranh.
Đoan Môn: Cửa chính dẫn vào Cấm Thành (nơi ở của nhà vua). Công trình được xây bằng gạch vồ thời Lê và sửa sang lại thời Nguyễn, cấu trúc cuốn vòm vững chãi với 3 cửa cuốn vòm.
Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-cot-co', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000015-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Đôi rồng đá Điện Kính Thiên', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc…', 'Điện Kính Thiên (Nền điện): Là hạt nhân trung tâm của hoàng cung qua các thời kỳ Lý - Trần - Lê. Hiện nay cung điện nguy nga xưa chỉ còn lại phần nền móng đá và Đôi rồng đá chầu được tạc từ thế kỷ XV (thời Lê Sơ) – một kiệt tác điêu khắc mang đậm dấu ấn quyền lực hoàng gia.
Bắc Môn (Chính Bắc Môn): Cửa thành duy nhất còn sót lại của thành Hà Nội thời Nguyễn. Trên mặt thành phía ngoài vẫn còn nguyên hai vết đại bác do pháo thuyền Pháp bắn vào từ sông Hồng năm 1882 khi hạ thành.
Nhà và Hầm D67: Công trình mang đậm dấu ấn lịch sử hiện đại. Căn hầm bê tông cốt thép kiên cố là nơi Bộ Chính trị và Quân ủy Trung ương đưa ra những quyết định lịch sử định đoạt vận mệnh đất nước (như Chiến dịch Điện Biên Phủ trên không năm 1972 hay Chiến dịch Hồ Chí Minh năm 1975).
B. Khu khảo cổ 18 Hoàng Diệu & Cổ vật quý hiếm
Nằm đối diện khu thành cổ, đây là hố khai quật khảo cổ phát lộ hàng triệu di vật chồng xếp lên nhau theo dòng thời gian:
Kiến trúc lòng đất: Các móng cột trụ bằng sỏi, đường đi lát gạch, hệ thống cống thoát nước từ thời Lý, Trần, Lê được làm xuất lộ rõ ràng.', 'artifact:hoang-thanh-thang-long-doi-rong-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000016-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Ngói chim phượng thời Lý', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn…', 'Các loại gạch ngói chạm khắc hình rồng, phượng tinh xảo, đặc biệt là ngói lá đề chim phượng tráng men xanh thời Lý.
Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.', 'artifact:hoang-thanh-thang-long-ngoi-phuong', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000017-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222204', 'Bát sứ ngự dụng', '/media/heritage/hoang-thanh-thang-long/cover.jpeg', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế,…', 'Đồ gốm sứ ngự dụng (dành riêng cho vua): Những chiếc bát sứ mỏng như vỏ trứng, lòng bát dập nổi hình rồng 5 móng và chữ "Quan" (đồ do lò quan đúc).
Giếng cổ thời Trần, thời Lê vẫn còn nguyên nguồn nước trong vắt.
4. Ứng dụng AR và Trải nghiệm số hóa tại Hoàng Thành
Tương tự như Đại Nội Huế, Hoàng Thành Thăng Long đã và đang ứng dụng rất mạnh mẽ công nghệ thực tế ảo và thực tế tăng cường (AR) để mang lịch sử đến gần hơn với du khách:
"Hồi sinh" Điện Kính Thiên bằng AR: Vì điện Kính Thiên hiện tại chỉ còn phần nền đá và rồng đá, du khách có thể dùng kính VR hoặc ứng dụng AR trên điện thoại để nhìn thấy toàn bộ kiến trúc gỗ quy mô 7 gian 2 chái, mái ngói chồng diêm của Điện Kính Thiên thời Lê Sơ hiển thị ngay tại vị trí thực tế với tỉ lệ 1:1.
Tour đêm "Giải mã Hoàng thành Thăng Long": Trong tour du lịch trải nghiệm này, công nghệ chiếu sáng nghệ thuật kết hợp cùng các ứng dụng quét mã tương tác giúp du khách có thể tự tay "giải mã" các cổ vật bằng cách chạm vào màn hình, xem dòng chảy lịch sử của hiện vật đó từ lúc được làm ra cho đến khi bị chôn vùi dưới lòng đất.
Xem các nghi lễ cung đình ảo: Bạn có thể quét các điểm AR tại Đoan Môn để chứng kiến đoàn rước hoàng gia, lính canh mặc giáp trụ di chuyển hay lễ ban sóc diễn ra ngay trước mắt thông qua màn hình thiết bị thông minh.
Hoàng Thành Thăng Long là một cuốn sử ký mở bằng đất đá và cổ vật. Việc tích hợp các công nghệ AR giúp người xem phá vỡ rào cản thời gian, dễ dàng hình dung ra một kinh thành Thăng Long vàng son, lộng lẫy nghìn năm trước.', 'artifact:hoang-thanh-thang-long-bat-su', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222205', 'Phố Cổ Hội An', '1. Tổng quan & Vị trí
Vị trí: Nằm ở hạ lưu sông Thu Bồn, thuộc địa phận thành phố Hội An, tỉnh Quảng Nam, cách thành phố Đà Nẵng khoảng 30km về phía Nam.
Tầm vóc: Hội An là một ví dụ đặc biệt xuất sắc về một thương cảng truyền thống ở Đông Nam Á được bảo tồn nguyên vẹn gần như cấu trúc đô thị ban đầu. Với những giá trị văn hóa, kiến trúc vật thể và phi vật thể vô giá, Phố cổ Hội An đã được UNESCO công nhận là Di sản Văn hóa Thế giới vào năm 1999.
2. Lịch sử hình thành: Từ cảng thị Champa đến thương cảng quốc tế
Thời kỳ Tiền Hội An: Từ thế kỷ thứ II đến thế kỷ XV, nơi đây từng là cảng biển sầm uất của vương quốc Champa (thời kỳ Lâm Ấp phố), đóng vai trò quan trọng trên Con đường tơ lụa và Con đường gia vị trên biển.
Thời kỳ Hưng thịnh (Thế kỷ XVI - XVIII): Dưới thời các Chúa Nguyễn mở mang bờ cõi về phía Nam (Đàng Trong), Hội An phát triển thành thương cảng quốc tế lớn nhất khu vực. Các thuyền buôn từ Nhật Bản, Trung Quốc, Bồ Đào Nha, Hà Lan, Ấn Độ... tấp nập đến đây trao đổi hàng hóa (lụa là, gốm sứ, trầm hương, lâm sản).
Sự giao thoa văn hóa: Trong thời kỳ này, nhiều thương nhân Trung Quốc và Nhật Bản đã được phép định cư, xây dựng các khu phố, hội quán và nhà ở theo phong cách riêng của họ, hòa quyện với nền tảng kiến trúc truyền thống của người Việt, tạo nên một diện mạo đô thị độc nhất vô nhị.
Thời kỳ suy thoái & May mắn của lịch sử: Đến thế kỷ XIX, do sông Thu Bồn bị bồi lấp trầm tích và nhà Nguyễn thực hiện chính sách đóng cửa (bế quan tỏa cảng), cộng thêm sự trỗi dậy của cảng biển Đà Nẵng do người Pháp xây dựng, Hội An dần mất đi vị thế kinh tế. Tuy nhiên, chính sự "lãng quên" này lại giúp Hội An may mắn thoát khỏi quá trình đô thị hóa và sự tàn phá của chiến tranh để giữ gìn nguyên vẹn cấu trúc khu phố cổ cho đến ngày nay.
3. Kiến trúc giao thoa & Những di sản kiến trúc nổi bật
Kiến trúc Hội An mang đặc trưng với những ngôi nhà hình ống (bề ngang hẹp, chiều sâu rất dài) tường sơn màu vàng rơm nghệ thuật, mái lợp ngói âm dương và cấu trúc khung gỗ chịu lực vững chắc.
Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.
Các Hội quán của người Hoa:
Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.
Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.
Trải nghiệm AR (Thực tế tăng cường) "Xuyên không về quá khứ":
Thông qua các ứng dụng di động thông minh tích hợp công nghệ AR, khi du khách đứng tại bờ sông Hoài hoặc trước các hội quán, bạn có thể quét camera để chứng kiến cảnh tượng thương cảng Hội An thế kỷ XVII tái hiện lại ngay trước mắt: các thuyền buôn gỗ mui buồm lớn của Nhật Bản, Bồ Đào Nha đang neo đậu; cảnh tiểu thương mặc trang phục cổ xưa tấp nập bốc dỡ hàng hóa trên bến dưới thuyền.
Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 15.879, 108.326, 'Quảng Nam', '/media/heritage/pho-co-hoi-an/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Phố Cổ Hội An
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000018-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Chùa Cầu (Lai Viễn Kiều)', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu…', 'Chùa Cầu (Lai Viễn Kiều):
Được các thương nhân Nhật Bản xây dựng vào đầu thế kỷ XVII, là biểu tượng trường tồn của phố cổ.
Đây là công trình kiến trúc độc đáo "trên cầu dưới chùa", mái che cong mềm mại, chạm khắc tinh xảo. Cầu bắc qua một con lạch nhỏ thông ra sông Thu Bồn. Giữa cầu là ngôi miếu nhỏ thờ Bắc Đế Trấn Vũ – vị thần chuyên trị phong ba, lũ lụt theo tâm linh người xưa.', 'artifact:pho-co-hoi-an-chua-cau', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000019-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Hội quán Phúc Kiến', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến…', 'Hội quán Phúc Kiến: Công trình quy mô và lộng lẫy nhất, thờ Thiên Hậu Thánh Mẫu (bà chúa đỡ đầu cho các thương thuyền vượt sóng gió). Hội quán nổi bật với cổng Tam quan rực rỡ, hòn non bộ và chính điện uy nghiêm.
Hội quán Quảng Đông, Triều Châu, Hải Nam: Mỗi hội quán mang một phong cách kiến trúc đặc trưng của từng vùng miền Trung Hoa với nghệ thuật khảm sành sứ và đắp nổi vô cùng sống động.', 'artifact:pho-co-hoi-an-hoi-quan-phuc-kien', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000020-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Nhà cổ Tân Ký', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ…', 'Các Nhà cổ trăm tuổi:
Nhà cổ Tân Ký, Nhà cổ Phùng Hưng, Nhà cổ Đức An: Đây là những ngôi nhà tư nhân có tuổi đời hơn 200 năm, nơi sinh sống của nhiều thế hệ trong một gia đình. Kiến trúc nhà kết hợp hài hòa giữa 3 nền văn hóa: Việt (mái lợp ngói âm dương, hệ cột kèo gỗ), Nhật (hệ thống xà gồ chồng rường mái hình thanh cua) và Trung Hoa (các chi tiết chạm khắc liễn đối, triện chữ). Trong nhà luôn có giếng trời ở giữa để lấy ánh sáng tự nhiên và điều hòa không khí.
4. Công nghệ AR và Xu hướng số hóa không gian Phố cổ
Hội An cổ kính nhưng không hề đứng ngoài dòng chảy công nghệ. Hiện nay, di sản này đang được ứng dụng mạnh mẽ các giải pháp số hóa để bảo tồn và thu hút du khách:
Số hóa 3D kiến trúc Phố cổ: Toàn bộ không gian các công trình đặc trưng như Chùa Cầu, Hội quán Phúc Kiến hay các ngôi nhà cổ đã được quét la-ze mặt đất (3D Laser Scanning) để lưu trữ dữ liệu kiến trúc chính xác đến từng milimet, phục vụ cho công tác trùng tu chính xác và hiển thị trên không gian số.', 'artifact:pho-co-hoi-an-nha-co-tan-ky', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000021-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222205', 'Tượng Khỉ và Chó đá', '/media/heritage/pho-co-hoi-an/cover.jpeg', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có…', 'Tại di tích Chùa Cầu, AR giúp người xem tương tác, nhìn thấy các cấu trúc gỗ ẩn bên trong móng mố cầu hoặc tìm hiểu lịch sử linh vật Thần Thú (Tượng Khỉ đá và Chó đá trấn giữ hai đầu cầu) một cách trực quan bằng hình ảnh chuyển động 3D.
Bản đồ du lịch thông minh & Tour ảo 360°: Khách du lịch có thể thực hiện chuyến tham quan trực tuyến Phố cổ Hội An từ xa qua các góc nhìn Panorama 360 độ sắc nét, kết hợp trợ lý giọng nói AI hướng dẫn tường tận về lịch sử của từng góc phố, từng làng nghề truyền thống (làng gốm Thanh Hà, làng rau Trà Quế).
Phố cổ Hội An vừa mang vẻ đẹp đằm thắm, dịu dàng của ánh đèn lồng lung linh ban đêm, vừa chứa đựng một chiều sâu lịch sử văn hóa đồ sộ, nay lại càng trở nên sống động hơn bao giờ hết nhờ sự hỗ trợ của các công nghệ AR và số hóa di sản đương đại.', 'artifact:pho-co-hoi-an-linh-vat-cau', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222206', 'Thành Nhà Hồ', '1. Tổng quan & Vị trí
Vị trí: Nằm trên địa phận hai xã Vĩnh Tiến và Vĩnh Long, thuộc huyện Vĩnh Lộc, tỉnh Thanh Hóa.
Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 20.077, 105.603, 'Thanh Hóa', '/media/heritage/thanh-nha-ho/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Thành Nhà Hồ
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000022-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Cổng Nam (Cổng Tiền)', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao:…', 'Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.
La Thành: Hệ thống thành đất vòng ngoài dài hơn 10km, nương theo điều kiện địa hình tự nhiên (sông Mã, sông Bưởi và núi non) để làm rào chắn bảo vệ kinh thành khỏi giặc ngoại xâm và lũ lụt.
Hào thành: Hệ thống hào nước sâu bao bọc ngay sát phía ngoài tường thành đá, nối trực tiếp với sông sông để điều tiết nước.
4. Tiềm năng ứng dụng công nghệ AR và Số hóa tại Thành Nhà Hồ
Dù các công trình điện thờ bằng gỗ bên trong Thành Nội đã bị thời gian và chiến tranh hủy hoại, nhưng công nghệ AR và số hóa đang mở ra cơ hội "hồi sinh" di sản này:
Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.', 'artifact:thanh-nha-ho-cong-nam', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000023-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Khối đá thạch anh', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông…', 'Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)
Cấu trúc: Thành được xây theo hình chữ nhật gần vuông (chiều Nam - Bắc dài 870,5m; Đông - Tây dài 883,5m).
Những khối đá khổng lồ: Tường thành được xây dựng bằng cách đục đẽo các khối đá thạch gọc vuông vức, có khối dài tới 6m, nặng hơn 20 tấn. Các phiến đá được mài nhẵn và xếp chồng lên nhau theo kỹ thuật mộng mẹo khít khao đến mức một lưỡi dao mỏng cũng không thể lách qua được. Mặt trong của tường thành được ốp bằng đất thện nện chặt kiên cố để chịu lực.
4 Cổng thành (Tiền - Hậu - Tả - Hữu): Thành có 4 cổng mở theo 4 hướng Đông, Tây, Nam, Bắc.
Cổng Nam (Cổng Tiền): Là cổng chính, đồ sộ nhất với kiến trúc 3 vòm cuốn bằng đá, cao hơn 8m. Các vòm cuốn được ghép bằng những phiến đá hình múi bưởi cực kỳ chuẩn xác về mặt hình học và kỹ thuật chịu lực. Ba cổng còn lại chỉ có 1 vòm cuốn.
B. Các công trình phụ cận và ngoại vi
Đàn tế Nam Giao: Nằm ở phía Nam ngoài thành (trên núi Đốn Sơn), là nơi vua nhà Hồ tổ chức các nghi lễ tế trời đất, cầu cho quốc thái dân an. Đây là đàn tế Nam Giao còn giữ được mặt bằng nền móng nguyên vẹn nhất trong lịch sử khảo cổ Việt Nam.', 'artifact:thanh-nha-ho-khoi-da', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000024-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Gạch in chữ Tây Đô', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng…', 'Tên gọi khác: Thành Tây Đô, An Tôn, Tây Kinh hay Thạch Thành.
Tầm vóc: Đây là tòa thành kiên cố bằng đá có một không hai tại Việt Nam và khu vực. Thành Nhà Hồ được UNESCO chính thức công nhận là Di sản Văn hóa Thế giới vào năm 2011 nhờ các giá trị nổi bật toàn cầu về kiến trúc, kỹ thuật xây dựng và cảnh quan phong thủy.
2. Lịch sử hình thành: Tòa thành xây thần tốc trong 3 tháng
Bối cảnh lịch sử (1397): Cuối thế kỷ XIV, triều đại nhà Trần suy yếu nghiêm trọng. Quyền thần Hồ Quý Ly (lúc này là Đại sư triều Trần) đã quyết định dời đô từ kinh thành Thăng Long (Hà Nội) về đất An Tôn (Thanh Hóa) nhằm chuẩn bị cho việc thay thế nhà Trần và xây dựng phòng tuyến kháng chiến chống quân Minh xâm lược.
Xây dựng thần tốc: Tòa thành được khởi công vào mùa xuân năm 1397 dưới sự chỉ huy của Thượng thư Lại bộ Thái sử lệnh Đỗ Tỉnh. Điều kinh ngạc là toàn bộ công trình hoàng thành khổng lồ này được xây dựng và hoàn thành chỉ trong vòng 3 tháng (từ tháng Giêng đến tháng Ba năm 1397). Đến năm 1400, Hồ Quý Ly chính thức lên ngôi vua, lập ra nhà Hồ và chọn nơi đây làm kinh đô của nước Đại Ngu.
3. Kỹ thuật xếp đá độc đáo & Các hạng mục công trình
Điểm cốt lõi làm nên giá trị toàn cầu của Thành Nhà Hồ chính là kỹ thuật khai thác, vận chuyển và xây dựng bằng các khối đá vôi khổng lồ mà không cần bất kỳ một chất kết dính nào (như vôi vữa hay mật mía).
A. Kiến trúc Hoàng Thành (Thành Nội)', 'artifact:thanh-nha-ho-gach-tay-do', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000025-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222206', 'Đạn đá thần cơ', '/media/heritage/thanh-nha-ho/cover.jpeg', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D…', 'Khám phá 3D cổ vật lòng đất: Qua các cuộc khai quật khảo cổ, các nhà khoa học đã tìm thấy hàng vạn viên gạch in chữ "Tây Đô", ngói mũi hài, các đầu chim phượng bằng đất nung và đặc biệt là hàng trăm quả đạn đá (dùng cho súng thần cơ của Hồ Nguyên Trừng). Các hiện vật này hiện đang được số hóa 3D giúp du khách tương tác xoay lật trên màn hình để nghiên cứu cấu trúc.
Tái hiện Điện Hoàng Nguyên bằng AR: Trong tương lai gần, công nghệ thực tế tăng cường (AR) sẽ giúp phục dựng lại không gian nội thành. Khi đứng trên nền đất trống hiện tại và nhìn qua màn hình thiết bị AR, du khách có thể thấy quần thể kiến trúc gỗ nguy nga như điện Hoàng Nguyên, cung Nhân Thọ... hiển thị chân thực ngay tại tọa độ gốc.
Giải mã kỹ thuật nâng đá bằng mô phỏng ảo: Ứng dụng công nghệ VR/AR để mô phỏng lại cách người thợ thủ công thế kỷ 15 vận chuyển các khối đá nặng hàng chục tấn từ núi An Tôn về thành và dùng hệ thống đòn bẩy, con lăn để đưa đá lên cao – giải mã bí ẩn xây thành chỉ trong 90 ngày cho khách tham quan.
Thành Nhà Hồ không chỉ là minh chứng cho một giai đoạn lịch sử đầy biến động mà còn là đài kỷ niệm tôn vinh tài năng kỹ nghệ, tư duy kiến trúc quân sự vượt thời gian của người Việt cổ.', 'artifact:thanh-nha-ho-dan-da', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222207', 'văn miếu quốc tử giám', '1. Tổng quan & Vị trí
Vị trí: Nằm tại số 58 phố Quốc Tử Giám, quận Đống Đa, Hà Nội. Khu di tích được bao bọc bởi 4 tuyến phố trung tâm: Nguyễn Thái Học, Tôn Đức Thắng, Văn Miếu và Quốc Tử Giám.
Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia
Toàn bộ khuôn viên Văn Miếu - Quốc Tử Giám được bao quanh bởi tường gạch vồ kiên cố, bố cục đối xứng từng khu vực theo trục chính từ Nam ra Bắc, mô phỏng theo khuôn mẫu của Văn Miếu Khổng Tử tại Trung Quốc nhưng mang đậm bản sắc kiến trúc dân tộc Việt Nam.
A. Trục tham quan chính (5 khu vực)
Khu thứ nhất (Từ cổng chính Văn Miếu Môn đến cổng Đại Trung Môn): Không gian mở với thảm cỏ xanh, cây cổ thụ, tạo cảm giác thanh tịnh tách biệt với phố thị ồn ào.
Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.
Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 21.027, 105.835, 'Hà Nội', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: văn miếu quốc tử giám
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000026-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Khuê Văn Các', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'Khu thứ hai (Khuê Văn Các):
Được xây dựng vào năm 1805 dưới thời Nguyễn. Đây là lầu vuông tám mái, bốn mặt có cửa sổ tròn lồng các thanh gỗ tỏa ra như vầng mặt trời tỏa sáng.
Khuê Văn Các (gác vẻ đẹp của sao Khuê - ngôi sao chủ về văn học) chính là biểu tượng chính thức của thủ đô Hà Nội.', 'artifact:van-mieu-quoc-tu-giam-khue-van-cac', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000027-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', '82 bia Tiến sĩ', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai…', 'Tầm vóc: Đây là một trong những khu di tích lịch sử - văn hóa trọng điểm của thủ đô và cả nước. Năm 2010, 82 bia Tiến sĩ tại đây đã được UNESCO công nhận là Di sản tư liệu thế giới thuộc Chương trình Ký ức Thế giới.
2. Lịch sử hình thành và Phát triển
Quần thể kiến trúc này thực chất gồm hai phần chính gắn liền với hai mốc thời gian lịch sử:
Văn Miếu (Năm 1070): Được vua Lý Thánh Tông cho xây dựng vào mùa thu năm Thần Vũ thứ 2 để thờ các bậc thánh hiền Nho giáo (Khổng Tử, Tứ Phối) và cũng là nơi hoàng thái tử (sau này là vua Lý Nhân Tông) đến học tập.
Quốc Tử Giám (Năm 1076): Vua Lý Nhân Tông cho xây dựng thêm Quốc Tử Giám ngay sau Văn Miếu. Ban đầu, trường chỉ dành riêng cho con em hoàng tộc và đại thần (nên gọi là Quốc Tử).
Mở rộng cửa cho nhân tài (Năm 1236): Sang thời nhà Trần, vua Trần Thái Tông đổi tên thành Quốc Học Viện và bắt đầu thu nhận cả những con em thường dân có học lực xuất sắc ở các địa phương.
Thời kỳ hưng thịnh (Thời Lê Sơ): Dưới triều vua Lê Thánh Tông, nền khoa bảng Nho học đạt đến đỉnh cao. Ông cho dựng các tấm bia Tiến sĩ đầu tiên (năm 1484) để vinh danh những người đỗ đại khoa, tạo nguồn động lực lớn cho các sĩ tử đương thời.
3. Cấu trúc không gian & Những báu vật quốc gia', 'artifact:van-mieu-quoc-tu-giam-bia-tien-si', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000028-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Hồ Thiên Quang', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự…', 'Khu thứ ba (Hồ Thiên Quang & Vườn bia Tiến sĩ):
Hồ Thiên Quang: Hồ nước hình vuông nằm ngay sau Khuê Văn Các, soi bóng giếng trời, mang ý nghĩa tụ hội tinh hoa đất trời.
82 tấm bia Tiến sĩ: Nằm đối xứng hai bên hồ, mỗi bên 41 bia. Các tấm bia đá được đặt trên lưng Rùa đá (biểu tượng của sự trường tồn, bền vững). Trên bia khắc tên tuổi, quê quán của 1.304 vị Tiến sĩ đỗ đạt qua các kỳ thi đại khoa từ năm 1442 đến 1779.
Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.', 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000029-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222207', 'Tượng Chu Văn An', '/media/heritage/van-mieu-quoc-tu-giam/cover.jpeg', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu…', 'Khu thứ tư (Cổng Đại Thành & Điện Đại Thành): Khu vực tâm linh chính, nơi thờ Khổng Tử và các học trò xuất sắc của ông. Tại đây cũng thờ Tư nghiệp Quốc Tử Giám Chu Văn An – người thầy mẫu mực, người được mệnh danh là "vạn thế sư biểu" (người thầy của muôn đời) của giáo dục Việt Nam.
Khu thứ sáu (Khu Thái Học): Nơi ngày xưa là trường học, nhà giảng đường, ký túc xá cho sĩ tử. Từng bị Pháp phá hủy hoàn toàn vào năm 1946, khu vực này đã được phục dựng quy mô vào năm 2000 để làm nơi tôn vinh, triển lãm các giá trị giáo dục xưa và nay.
4. Số hóa di sản và các trải nghiệm Công nghệ
Không đứng ngoài làn sóng số hóa, Văn Miếu - Quốc Tử Giám hiện là một trong những không gian di sản chuyển đổi số thành công bậc nhất tại Hà Nội:
Hệ thống thuyết minh tự động (Audio Guide): Hỗ trợ nhiều ngôn ngữ, giúp du khách tự trải nghiệm sâu sắc từng câu chuyện lịch sử của 82 bia Tiến sĩ thông qua thiết bị cầm tay hoặc quét mã QR trên điện thoại.
Trình chiếu công nghệ 3D Mapping (Tour đêm Văn Miếu): Đây là điểm nhấn công nghệ cực kỳ ấn tượng. Vào ban đêm, toàn bộ mặt tiền của khu Thái Học hay Khuê Văn Các trở thành một "màn hình khổng lồ" trình chiếu ánh sáng và âm thanh 3D, tái hiện lại quá trình lều chọi đi thi, vinh quy bái tổ của các sĩ tử ngày xưa một cách vô cùng sống động.
Số hóa tương tác tương lai: Du khách có thể trải nghiệm lớp học thầy đồ ảo, tự tay viết chữ thư pháp trên màn hình tương tác cảm ứng, hoặc ngắm nhìn các hoa văn điêu khắc rùa đá, bia đá được dựng hình 3D xoay 360 độ cực kỳ sắc nét.
Văn Miếu - Quốc Tử Giám không chỉ là nơi để cầu may mắn, đỗ đạt trước mỗi mùa thi, mà còn là không gian văn hóa kết nối quá khứ huy hoàng với hiện tại bằng những trải nghiệm công nghệ đầy sáng tạo.', 'artifact:van-mieu-quoc-tu-giam-chu-van-an', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222208', 'Đại nội Huế', '1. Tổng quan & Vị trí
Vị trí: Nằm ở bờ Bắc sông Hương, thuộc địa phận kinh thành Huế.
Khái niệm: Đại Nội là cụm di tích bao gồm Hoàng Thành (nơi vua thiết triều, làm việc và các cung điện thờ tự) và Tử Cấm Thành (nơi sinh hoạt, ăn ở của vua và hoàng gia).
Tầm vóc: Đây là công trình có quy mô đồ sộ nhất trong lịch sử Việt Nam, mất gần 30 năm với hàng vạn người tham gia xây dựng và là hạt nhân của Quần thể di tích Cố đô Huế (được UNESCO công nhận là Di sản Văn hóa Thế giới từ năm 1993).
2. Lịch sử hình thành
Khởi dựng (1804): Sau khi lên ngôi và thống nhất đất nước, vua Gia Long đã khảo sát và chọn khu đất bên bờ sông Hương để xây dựng kinh thành mới. Việc xây dựng Hoàng Thành bắt đầu từ mùa hè năm 1804.
Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.
Cửu Đỉnh: Đặt trước sân Thế Miếu, gồm 9 chiếc đỉnh đồng lớn được đúc dưới thời vua Minh Mạng (1835). Mỗi chiếc đỉnh mang tên hiệu của một vị vua và được chạm khắc tinh xảo các hình ảnh biểu tượng cho giang sơn, sông núi, sản vật của Việt Nam. Đây là những bảo vật quốc gia có một không hai.
Hiển Lâm Các: Công trình kiến trúc bằng gỗ cao 3 tầng, được ví như đài kỷ niệm ghi nhớ công ơn của các bậc công thần đã có đóng góp cho triều đại.
4. Công nghệ AR & Trải nghiệm thực tế ảo tại Đại Nội
Như dòng trạng thái bạn chia sẻ ("mở khóa trải nghiệm thực tế ảo"), Đại Nội Huế hiện nay là một trong những điểm đi đầu tại Việt Nam trong việc ứng dụng công nghệ vào du lịch di sản:
Trải nghiệm AR (Augmented Reality - Thực tế tăng cường): Du khách có thể dùng điện thoại thông minh hoặc máy tính bảng quét các bảng mã tại điểm di tích để "hồi sinh" các cung điện đã bị chiến tranh phá hủy. Bạn sẽ nhìn thấy kiến trúc 3D nguyên bản của Điện Cần Chánh, Điện Kiến Trung... hiện lên ngay trên nền móng đổ nát cũ một cách sống động.
Không gian ảo VR 3D (Virtual Reality): Tại khu vực Trung tâm Phục dựng Di sản, du khách có thể đeo kính VR để trải nghiệm tour tham quan xuyên không gian, tìm hiểu về lối sống cung đình, xem các nghi lễ triều chính mô phỏng như thật.
Quét mã ngắm cổ vật: Các bảo vật quốc gia như Cửu Đỉnh hay các hiện vật trong điện thờ đều được số hóa, cho phép người xem xoay 360 độ trên thiết bị để nhìn rõ từng đường nét chạm khắc nhỏ nhất.
Đại Nội Huế không chỉ là một chuyến đi tham quan lịch sử đơn thuần, mà là sự giao thoa giữa giá trị cổ kính cốt lõi và dòng chảy công nghệ hiện đại, giúp thế hệ trẻ dễ dàng tiếp cận quá khứ một cách trực quan nhất.', 16.469, 107.579, 'Huế', '/media/heritage/dai-noi-hue/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đại nội Huế
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000030-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngọ Môn', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-ngo-mon', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000031-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Ngai Vàng Điện Thái Hòa', '/media/heritage/dai-noi-hue/cover.jpeg', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ…', 'Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự
Thế Miếu (Thế Tổ Miếu): Nơi thờ các vị vua triều Nguyễn.', 'artifact:dai-noi-hue-ngai-vang', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000032-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Cửu Đỉnh', '/media/heritage/dai-noi-hue/cover.jpeg', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng…', 'Hoàn thiện (1833): Quy hoạch và kiến trúc của Đại Nội chỉ thực sự hoàn chỉnh dưới thời vua Minh Mạng vào năm 1833, khi hàng loạt cung điện, lầu các và hệ thống tường thành bao quanh được hoàn thành.
Biến động lịch sử: Trải qua 143 năm là thủ đô của triều Nguyễn (1802 – 1945), Đại Nội đã chứng kiến nhiều thăng trầm. Do tác động của chiến tranh (đặc biệt là các sự kiện năm 1947 và 1968), nhiều công trình nguy nga bên trong từng bị tàn phá, san phẳng hoàn toàn. Những năm gần đây, công tác trùng tu đang được đẩy mạnh để phục dựng lại nguyên bản các điện thờ lớn.
3. Cấu trúc không gian & Các công trình tiêu biểu
Đại Nội được xây dựng theo sơ đồ gần như hình vuông, chu vi khoảng 2,5km, mặt mặt hướng về phía Nam.
A. Hệ thống cửa ngõ & Khu vực thiết triều (Hoàng Thành)
Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)', 'artifact:dai-noi-hue-cuu-dinh', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000033-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222208', 'Lầu Ngũ Phụng', '/media/heritage/dai-noi-hue/cover.jpeg', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua…', 'Ngọ Môn (Cửa chính): Cửa lớn nhất nằm ở phía Nam, chỉ dành riêng cho vua đi lại hoặc là nơi tổ chức các buổi lễ rầm rộ như Lễ Truyền Lô (xướng danh tiến sĩ), Lễ Ban Sóc (phát lịch mới). Phía trên Ngọ Môn là Lầu Ngũ Phụng với kết cấu gỗ hai tầng cực kỳ bề thế và thanh thoát. Đây cũng là nơi vua Bảo Đại thoái vị vào năm 1945, chấm dứt chế độ phong kiến Việt Nam.
Điện Thái Hòa & Sân Đại Triều Nghi: Nơi đặt Ngai Vàng của 13 vị vua triều Nguyễn. Đây là trung tâm của toàn bộ kinh thành, nơi diễn ra các buổi đại triều vào ngày mồng 1 và ngày rằm hàng tháng, hoặc lễ đăng quang của các hoàng đế. Điện được xây theo lối "trùng thiềm điệp ốc" (nhà nối nhà) với hệ thống cột gỗ sơn son thếp vàng chạm khắc rồng uốn lượn.
B. Khu vực sinh hoạt của Hoàng gia (Tử Cấm Thành)
Nằm ngay sau điện Thái Hòa, được giới hạn bởi bức tường thành bảo vệ nghiêm ngặt.
Tả Vu & Hữu Vu: Hai tòa nhà đối xứng ngay sau điện Thái Hòa, nơi các quan chuẩn bị áo mũ chỉnh tề trước khi vào chầu vua.
Điện Cần Chánh & Điện Càn Thành: Nơi vua làm việc hàng ngày và nơi ngủ nghỉ của nhà vua (đang trong quá trình phục dựng).
Cung Diên Thọ & Cung Trường Sanh: Nằm ở phía Tây của Tử Cấm Thành, là nơi ở và sinh hoạt của Hoàng Thái Hậu (mẹ vua) và Thái Hoàng Thái Hậu (bà nội vua). Các công trình này mang vẻ đẹp thanh nhã, có hồ nước và sân vườn rất thoáng đãng.
C. Khu vực thờ tự', 'artifact:dai-noi-hue-lau-ngu-phung', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('22222222-2222-2222-2222-222222222209', 'Đền Hùng Vương', '1. Tổng quan & Vị trí
Vị trí: Nằm trên núi Nghĩa Lĩnh (núi Hùng), thuộc xã Hy Cương, thành phố Việt Trì, tỉnh Phú Thọ, cách thủ đô Hà Nội khoảng 85km về phía Tây Bắc.
Tầm vóc: Đây là khu di tích lịch sử đặc biệt cấp quốc gia, trung tâm thờ tự tổ tiên của người Việt. Năm 2012, Tín ngưỡng thờ cúng Hùng Vương ở Phú Thọ đã được UNESCO công nhận là Di sản văn hóa phi vật thể đại diện của nhân loại.
Cảnh quan: Núi Hùng cao khoảng 175m, xung quanh là hệ thống đồi gò trùng điệp (truyền thuyết gọi là 99 con voi quay đầu về chầu nghĩa quân). Không gian nơi đây quanh năm mây mờ bao phủ, cây cối cổ thụ xanh tươi, mang đậm màu sắc huyền thoại, linh thiêng.
2. Lịch sử hình thành & Giá trị cội nguồn
Thời kỳ lập nước: Theo truyền thuyết, nơi đây chính là trung tâm kinh đô Phong Châu của nước Văn Lang cổ đại. Các vị vua Hùng đã chọn đỉnh núi Nghĩa Lĩnh cao nhất vùng để làm nơi tiến hành các nghi lễ tín ngưỡng bản địa: tế trời đất, tế thần lúa, cầu mong mưa thuận gió hòa, mùa màng bội thu.
Xây dựng qua các triều đại: Hệ thống kiến trúc lăng tẩm, đền đài được xây dựng từng bước và trùng tu lớn qua các thời kỳ Lý, Trần, Lê, Nguyễn. Công trình hiện nay phần lớn mang phong cách kiến trúc thời hậu Lê và thời Nguyễn.
Ngày Quốc Giỗ: Hằng năm, vào ngày mồng 10 tháng Ba âm lịch, hàng triệu người con đất Việt từ khắp mọi miền tổ quốc và kiều bào nước ngoài lại hướng về Đền Hùng để làm lễ dâng hương, thể hiện truyền thống đạo lý "Uống nước nhớ nguồn".
3. Cấu trúc không gian & Hệ thống đền đài từ chân lên đỉnh núi
Hành trình tham quan Đền Hùng là một trải nghiệm leo núi thiêng qua từng bậc đá cổ kính, tương ứng với các tầng di tích:
Đại Môn (Cổng đền): Được xây dựng vào năm 1917 dưới thời vua Khải Định. Cổng có kiến trúc kiểu vòm cuốn nguy nga, trên đỉnh đắp phù điêu hai chiến sĩ cầm chùy canh giữ, chính giữa khắc bốn chữ Hán lớn: Cao sơn cảnh hành (ý nói đức lớn của các vua Hùng cao như núi, đường lối của vua như đường lớn để mọi người noi theo).
Đền Hạ: Nằm ở tầng núi thấp nhất. Theo truyền thuyết, đây là nơi mẹ Âu Cơ đã hạ sinh ra bọc trăm trứng, sau đó nở thành một trăm người con trai, cội nguồn của hai tiếng "đồng bào". Ngay cạnh đền là Chùa Thiên Quang cổ kính.
Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:
Trải nghiệm AR "Hồi sinh không gian huyền thoại":
Khi du khách đứng tại Đền Hạ, việc sử dụng thiết bị quét mã AR sẽ tái hiện lại sống động bằng hoạt họa 3D câu chuyện bọc trăm trứng nở thành trăm người con, hay hình ảnh đoàn người lên núi tế trời đất thời cổ đại.
Tại Đền Trung, AR giúp mô phỏng lại hình ảnh Lang Liêu dâng chiếc bánh chưng, bánh giầy vuông tròn tượng trưng cho trời và đất lên vua cha, giúp bài học lịch sử trở nên thu hút, dễ hiểu đối với các bạn trẻ.
Bản đồ số hóa & Thuyết minh tự động: Hệ thống bản đồ 3D hiển thị chi tiết số lượng bậc đá, tọa độ các đền từ chân núi lên đỉnh núi Nghĩa Lĩnh. Du khách có thể dùng điện thoại tự quét mã tại mỗi điểm để nghe trợ lý AI thuyết minh tường tận về các truyền thuyết, ý nghĩa của từng câu đối, hoành phi cổ.
Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 21.383, 105.298, 'Phú Thọ', '/media/heritage/den-hung-vuong/cover.webp')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Artifacts from docx: Đền Hùng Vương
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000034-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Trống đồng Đông Sơn', '/media/heritage/den-hung-vuong/cover.webp', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc…', 'Bảo tàng Hùng Vương ảo: Các cổ vật quý hiếm thời kỳ văn hóa Đông Sơn (trống đồng, rìu chiến, dao găm, đồ ngọc ngự dụng) tìm thấy xung quanh khu vực kinh đô Phong Châu cũ đều được quét 3D. Du khách có thể tương tác xoay lật, phóng to hoa văn chim lạc trên trống đồng qua màn hình ảo một cách sắc nét.
Đền Hùng không chỉ là một danh thắng, mà là một không gian tâm linh linh thiêng bậc nhất, nơi nhắc nhở mỗi người Việt về nguồn cội chung của mình. Sự kết hợp của công nghệ AR như một chiếc cầu nối, giúp các câu chuyện huyền thoại nghìn năm hiển hiện một cách chân thực ngay giữa đời thực.', 'artifact:den-hung-vuong-trong-dong', 'primary', 1)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000035-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Bánh chưng, bánh giầy', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên…', 'Đền Trung (Hùng Vương Tổ miếu): Nơi các vua Hùng cùng các lạc hầu, lạc tướng thường lên ngắm cảnh thiên nhiên và bàn việc nước. Đây cũng là nơi gắn liền với câu chuyện vua Hùng thứ 6 nhường ngôi cho Lang Liêu nhờ việc sáng tạo ra bánh chưng, bánh giầy.
Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-banh-chung', 'primary', 2)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000036-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Cột đá thề An Dương Vương', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng…', 'Đền Thượng (Kính Thiên lĩnh điện): Nằm trên đỉnh núi cao nhất. Nơi các vua Hùng lập đàn tế trời đất. Phía bên cạnh đền có Lăng Hùng Vương (tương truyền là mộ của vị vua Hùng thứ 6). Tại cửa đền Thượng có cột đá thề do Thục Phán (An Dương Vương) dựng lên sau khi được nhường ngôi, nguyện một lòng bảo vệ giang sơn mà các vua Hùng đã gầy dựng.
Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-cot-da-the', 'primary', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES ('b0000037-0000-4000-8000-000000000001', '22222222-2222-2222-2222-222222222209', 'Giếng Ngọc', '/media/heritage/den-hung-vuong/cover.webp', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm"…', 'Đền Giếng: Nằm ở chân núi phía Đông Nam. Nơi đây có giếng Ngọc quanh năm nước trong vắt. Truyền thuyết kể rằng hai nàng công chúa Tiên Dung và Ngọc Hoa (con gái vua Hùng thứ 18) thường soi gương, chải tóc tại đây.
4. Công nghệ AR và Số hóa hành trình về Đất Tổ
Nhằm giúp du khách có thể "chạm" vào không gian huyền thoại một cách trực quan, Đền Hùng đã tích hợp nhiều ứng dụng công nghệ hiện đại:', 'artifact:den-hung-vuong-gieng-ngoc', 'primary', 4)
ON CONFLICT (location_id, unlock_key) DO UPDATE SET
  name = EXCLUDED.name, image_url = EXCLUDED.image_url, description = EXCLUDED.description,
  story = EXCLUDED.story, sort_order = EXCLUDED.sort_order;

INSERT INTO locations (id, name, description, latitude, longitude, city, cover_image)
VALUES ('11111111-1111-1111-1111-111111111111', 'Địa đạo Củ Chi', '1. Tổng quan & Vị trí
Vị trí: Thuộc huyện Củ Chi, TP. Hồ Chí Minh, cách trung tâm thành phố khoảng 70km về phía Tây Bắc. Khu di tích hiện nay được bảo tồn ở hai địa điểm chính: Địa đạo Bến Dược (Căn cứ Khu ủy và Bộ Tư lệnh Quân khu Sài Gòn - Chợ Lớn - Gia Định) và Địa đạo Bến Đình (Căn cứ Huyện ủy Củ Chi).
Tầm vóc: Đây là một hệ thống phòng thủ, căn cứ quân sự trong lòng đất vô cùng đồ sộ. Địa đạo Củ Chi đã được xếp hạng Di sản cấp quốc gia đặc biệt và đang trong quá trình hoàn thiện hồ sơ đệ trình UNESCO công nhận là Di sản Thế giới.
Danh hiệu: Nơi đây được mệnh danh là "Mảnh đất thép thành đồng" nhờ khả năng đứng vững trước hàng triệu tấn bom đạn của kẻ thù.
2. Lịch sử hình thành: Kỳ tích từ những chiếc cuốc thô sơ
Khởi nguồn (1946 - 1948): Hệ thống địa đạo bắt đầu được đào từ thời kỳ kháng chiến chống Pháp. Ban đầu, các xã chỉ đào những hầm bí mật đơn lẻ để ẩn nấp cán bộ và cất giấu tài liệu.
Mở rộng thành "Mạng nhện" (1961 - 1967): Bước sang cuộc kháng chiến chống Mỹ, trước sự càn quét khốc liệt của địch, hệ thống này được phát triển, nối liền các xã với nhau tạo thành một mạng lưới liên hoàn trong lòng đất. Quân và dân Củ Chi đã dùng những công cụ thô sơ như cuốc, xẻng và sọt tre để kiên trì đào xuyên lòng đất ngày đêm, tạo nên một "thành phố ngầm" kỳ vĩ.
3. Kiến trúc lòng đất độc đáo "Bất khả xâm phạm"
Hệ thống địa đạo Củ Chi có tổng chiều dài toàn tuyến lên đến hơn 250 km, chạy ngoằn ngoèo trong lòng đất đất sét pha đá sỏi (loại đất chịu lực tốt, càng ra gió càng cứng, không bị sập).
Cấu trúc 3 tầng biệt lập:
Tầng 1 (Cách mặt đất 3m): Chịu được đạn pháo và xe tăng, xe bọc thép cán qua.
Tầng 2 (Cách mặt đất 6m): Chịu được các loại bom cỡ nhỏ.
Tầng 3 (Cách mặt đất từ 8 - 12m): Tầng sâu nhất, cực kỳ an toàn, bất chấp cả bom cỡ lớn.
Hệ thống phòng ốc liên hoàn: Bên trong địa đạo không chỉ là đường đi lối lại chật hẹp mà còn có đầy đủ các không gian sinh hoạt: phòng họp của Bộ tư lệnh, hầm trú ẩn cho người già và trẻ em, công xưởng chế tạo vũ khí, nhà thương (bệnh viện dã chiến), nhà bếp, giếng nước và kho chứa lương thực.
Bếp Hoàng Cầm: Một phát minh vĩ đại của thời chiến được áp dụng triệt để tại đây. Bếp nấu ăn dưới lòng đất nhưng hệ thống dẫn khói được tản ra đi là là trên mặt đất qua các rãnh nhỏ cách xa bếp, giúp khói tan rã hoàn toàn vào sương sớm, khiến máy bay địch không thể phát hiện ra vị trí trú ẩn.
Lỗ thông hơi và Hệ thống hầm chông: Các lỗ thông hơi được ngụy trang khéo léo thành những tổ mối, gốc cây để lấy dưỡng khí. Bao quanh các lối vào địa đạo là mạng lưới mìn tự tạo và bẫy chông tre, chông sắt dày đặc nhằm ngăn chặn sự xâm nhập của lính thám báo.
4. Ứng dụng AR và Trải nghiệm số hóa tại Địa đạo Củ Chi
Vì địa đạo nằm sâu trong lòng đất, không gian tối và chật hẹp khiến việc tham quan thực tế đôi khi gặp hạn chế đối với một số du khách (người sợ không gian kín hoặc hạn chế về sức khỏe). Công nghệ AR và số hóa đã giải quyết xuất sắc bài toán này:
Mô phỏng "Cắt lớp lòng đất" bằng AR:
Khi đứng trên mặt đất, du khách chỉ cần dùng điện thoại hoặc kính thông minh quét không gian, ứng dụng AR sẽ hiển thị một sơ đồ 3D "xuyên thấu" mặt đất. Bạn sẽ nhìn thấy rõ ràng cấu trúc 3 tầng của địa đạo đang chạy ngay dưới chân mình, thấy cách các chiến sĩ di chuyển, hội họp và sinh hoạt bên dưới một cách trực quan.
AR cũng mô phỏng nguyên lý hoạt động giấu khói thần kỳ của Bếp Hoàng Cầm hay cách hoạt động của các loại bẫy chông ngụy trang một cách sống động mà không cần phải chạm vào hiện vật thật.
Tour tham quan ảo 360° dưới lòng đất: Đối với những du khách không thể xuống lòng đất, hệ thống quét hình ảnh 360 độ độ phân giải cao kết hợp với âm thanh giả lập (tiếng bom đạn, tiếng cuốc đất, tiếng thì thầm thời chiến) giúp họ có trải nghiệm như đang thực sự len lỏi qua từng vách đất của hầm chỉ huy Bến Dược.
Số hóa vũ khí tự chế: Du khách có thể tương tác với các mô hình 3D của các loại vũ khí tự chế độc đáo của du kích Củ Chi (như mìn gạt làm từ vỏ bom lép của địch, chông bập bênh...) trên màn hình cảm ứng, tìm hiểu cơ chế hoạt động lịch sử của chúng.
Địa đạo Củ Chi là một trường ca về lòng yêu nước và sự sáng tạo không giới hạn của con người. Sự hỗ trợ của công nghệ AR giống như một chiếc kính xuyên không, giúp chúng ta nhìn thấu lòng đất để thấu hiểu và tri ân một thời kỳ gian khổ nhưng vô cùng vẻ vang của thế hệ đi trước.', 11.143, 106.461, 'TP.HCM', '/media/heritage/dia-dao-cu-chi/cover.jpeg')
ON CONFLICT (id) DO UPDATE SET
  name = EXCLUDED.name,
  description = EXCLUDED.description,
  latitude = EXCLUDED.latitude,
  longitude = EXCLUDED.longitude,
  city = EXCLUDED.city,
  cover_image = EXCLUDED.cover_image;

-- Restore the final quest-v2 copy without rolling back any progress or structural columns.
UPDATE quests q
SET title = CASE WHEN q.title LIKE '%?%' THEN v.title ELSE q.title END,
    description = CASE WHEN q.description LIKE '%?%' THEN v.description ELSE q.description END,
    story = CASE WHEN q.story LIKE '%?%' THEN v.story ELSE q.story END
FROM (VALUES
    ('33333333-3333-3333-3333-333333333333'::uuid,
     'Hành trình dưới lòng đất',
     'Đi tour 360° qua Bếp Hoàng Cầm và Phòng họp, rồi so sánh Bếp Hoàng Cầm xưa và nay. Có Premium: xem thêm chương 1948 trong Cổng thời gian.',
     'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.'),
    ('33333333-3333-3333-3333-333333333334'::uuid,
     'Mật mã Sa bàn Tác chiến',
     'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.',
     'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".'),
    ('33333333-3333-3333-3333-333333333335'::uuid,
     'Ánh sáng Đèn Mù U',
     'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.',
     'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.'),
    ('44444444-4444-4444-4444-444444444401'::uuid,
     'Bước chân ra thế giới', 'Chiêm ngưỡng rồng trên mái, rồi khám phá chiếc vali mây.',
     'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Nhìn rồng trên mái, rồi khám phá vali mây.'),
    ('44444444-4444-4444-4444-444444444402'::uuid,
     'Ngọn tháp bên sông Hương', 'Tháp Phước Duyên và bia đá trên lưng rùa.',
     'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chiêm ngưỡng tháp, rồi mở bia đá.'),
    ('44444444-4444-4444-4444-444444444403'::uuid,
     'Dấu ấn kinh đô đầu tiên', 'Long Sàng đá và đền Lê Đại Hành.',
     '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Khám phá Long Sàng, rồi mở đền Lê Đại Hành.'),
    ('44444444-4444-4444-4444-444444444404'::uuid,
     'Dấu ấn Hoàng Thành', 'Móng cột cổ và ngói men xanh.',
     'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp hiện vật.'),
    ('44444444-4444-4444-4444-444444444405'::uuid,
     'Bí ẩn Chùa Cầu', 'Chùa Cầu và nhà cổ Tấn Ký.',
     'Hội An không chỉ đẹp trong ảnh. Nhìn Chùa Cầu, rồi mở nhà Tấn Ký.'),
    ('44444444-4444-4444-4444-444444444406'::uuid,
     'Cổ thành đá Tây Đô', 'Cổng Nam và gạch in chữ Tây Đô.',
     'Thành không vữa nhưng đứng vững 600 năm. Khám phá cổng Nam, rồi gạch in chữ Tây Đô.'),
    ('44444444-4444-4444-4444-444444444407'::uuid,
     'Đường đến khoa bảng', 'Khuê Văn Các và Hồ Thiên Quang.',
     'Chiêm ngưỡng Khuê Văn Các, rồi mở Hồ Thiên Quang.'),
    ('44444444-4444-4444-4444-444444444408'::uuid,
     'Bí ẩn Đại Nội', 'Ngọ Môn và Cửu Đỉnh.',
     'Chiêm ngưỡng Ngọ Môn, rồi mở Cửu Đỉnh.'),
    ('44444444-4444-4444-4444-444444444409'::uuid,
     'Về Đất Tổ', 'Trống đồng và cột đá thề.',
     'Chiêm ngưỡng trống đồng, rồi mở cột đá thề.')
) AS v(id, title, description, story)
WHERE q.id = v.id
  AND (q.title LIKE '%?%' OR q.description LIKE '%?%' OR q.story LIKE '%?%');

-- Labels used by quest locks and discovery cards come from discovery_points, not quest_steps.
UPDATE discovery_points p
SET name = v.name
FROM (VALUES
    ('scene:22222222-2222-2222-2222-222222222222', 'Cổng vào'),
    ('scene:22222222-2222-2222-2222-222222222221', 'Bếp Hoàng Cầm'),
    ('scene:22222222-2222-2222-2222-222222222223', 'Phòng họp'),
    ('photo:cua-ham', 'Cửa hầm'),
    ('photo:gieng', 'Giếng nước'),
    ('artifact:vu-khi', 'Hầm chế tạo vũ khí'),
    ('artifact:tram-xa', 'Trạm xá'),
    ('hotspot:vent', 'Lỗ thông hơi'),
    ('artifact:bay', 'Bẫy du kích'),
    ('era:1948', '1948 — khởi đầu hầm ngầm'),
    ('era:1968', 'Tết Mậu Thân 1968'),
    ('era:2026', 'Khu di tích 2026'),
    ('artifact:ben-nha-rong-rong-mai', 'Hai con rồng trên mái'),
    ('artifact:ben-nha-rong-vali-may', 'Chiếc vali mây'),
    ('artifact:chua-thien-mu-thap-phuoc-duyen', 'Tháp Phước Duyên'),
    ('artifact:chua-thien-mu-bia-da', 'Bia đá trên lưng rùa'),
    ('artifact:co-do-hoa-lu-long-sang', 'Long Sàng đá'),
    ('artifact:co-do-hoa-lu-den-le-dai-hanh', 'Đền Lê Đại Hành'),
    ('artifact:hoang-thanh-thang-long-cot-co', 'Móng cột cổ'),
    ('artifact:hoang-thanh-thang-long-ngoi-phuong', 'Ngói men xanh'),
    ('artifact:pho-co-hoi-an-chua-cau', 'Chùa Cầu'),
    ('artifact:pho-co-hoi-an-nha-co-tan-ky', 'Nhà cổ Tấn Ký'),
    ('artifact:thanh-nha-ho-cong-nam', 'Cổng Nam'),
    ('artifact:thanh-nha-ho-gach-tay-do', 'Gạch in chữ Tây Đô'),
    ('artifact:van-mieu-quoc-tu-giam-khue-van-cac', 'Khuê Văn Các'),
    ('artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 'Hồ Thiên Quang'),
    ('artifact:dai-noi-hue-ngo-mon', 'Ngọ Môn'),
    ('artifact:dai-noi-hue-cuu-dinh', 'Cửu Đỉnh'),
    ('artifact:den-hung-vuong-trong-dong', 'Trống đồng Đông Sơn'),
    ('artifact:den-hung-vuong-cot-da-the', 'Cột đá thề')
) AS v(unlock_key, name)
WHERE p.unlock_key = v.unlock_key
  AND (v.unlock_key NOT IN ('era:1948', 'era:1968', 'era:2026')
       OR p.location_id = '11111111-1111-1111-1111-111111111111'::uuid)
  AND p.name LIKE '%?%';

-- END migration: V88__repair_vietnamese_content.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V89__ar_card_mail_attempts.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V89__ar_card_mail_attempts.sql
-- ============================================================================
CREATE TABLE IF NOT EXISTS ar_card_mail_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    state VARCHAR(16) NOT NULL CHECK (state IN ('CREATED','ACCEPTED','UNKNOWN')),
    CONSTRAINT uq_ar_card_mail_user_request UNIQUE (user_id, request_id)
);
CREATE INDEX IF NOT EXISTS idx_ar_card_mail_user_created ON ar_card_mail_attempts(user_id, created_at DESC);

-- END migration: V89__ar_card_mail_attempts.sql
COMMIT;
BEGIN;

-- ============================================================================
-- BEGIN migration: V90__ar_card_mail_artifact.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\src\main\resources\db\migration\V90__ar_card_mail_artifact.sql
-- ============================================================================
-- Existing NULL values represent historical bulk-card emails; never assign them to an artifact.
ALTER TABLE ar_card_mail_attempts ADD COLUMN IF NOT EXISTS artifact_id VARCHAR(32);

-- END migration: V90__ar_card_mail_artifact.sql
COMMIT;

-- ============================================================================
-- BEGIN seed: 2026-10-15_admin_dashboard_revenue_demo.sql
-- Source: D:\FPT\SU26\EXE101\HistAR\BE\docs\sql\seed\2026-10-15_admin_dashboard_revenue_demo.sql
-- ============================================================================
-- Demo revenue for Admin dashboard chart (Stitch-like 6-month B2C/B2B).
-- Idempotent: keyed by order_code / transfer_content prefix SEED-DASH-*.
-- Depends on: 2026-07-10_demo_billing_accounts.sql (premium@histar.vn, demo-standard-org).
-- Amounts approximate Stitch wireframe (T5–T10) so the dashboard bar chart is visually readable.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure payer profiles/org exist (no-op if already seeded).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM profiles WHERE email = 'premium@histar.vn') THEN
    RAISE EXCEPTION 'Missing premium@histar.vn — run 2026-07-10_demo_billing_accounts.sql first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-standard-org') THEN
    RAISE EXCEPTION 'Missing demo-standard-org — run 2026-07-10_demo_billing_accounts.sql first';
  END IF;
END $$;

-- Paid B2C subscription so MRR/ARR cards are non-zero (DEMO method is excluded from MRR).
UPDATE b2c_subscriptions
SET payment_method = 'SEPAY',
    price_vnd = 79000,
    is_active = TRUE,
    end_date = DATE '2099-12-31'
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- Extra paid B2C subs for MRR demo volume (synthetic users optional — skip if not present).
-- Keep focused on payment transactions for the chart.

DELETE FROM b2c_payment_transactions WHERE order_code LIKE 'SEED-DASH-B2C-%';
DELETE FROM org_payment_transactions WHERE order_code LIKE 'SEED-DASH-B2B-%';

-- Month offsets relative to Asia/Ho_Chi_Minh "now": 5..0 months ago.
-- Targets (VND): B2C [520M, 610M, 780M, 690M, 890M, 1025M], B2B [350M, 430M, 560M, 650M, 760M, 820200000]
WITH months AS (
  SELECT * FROM (VALUES
    (5, 520000000, 350000000),
    (4, 610000000, 430000000),
    (3, 780000000, 560000000),
    (2, 690000000, 650000000),
    (1, 890000000, 760000000),
    (0, 1025000000, 820200000)
  ) AS t(offset_months, b2c_vnd, b2b_vnd)
),
payer AS (
  SELECT id AS user_id FROM profiles WHERE email = 'premium@histar.vn' LIMIT 1
),
org AS (
  SELECT o.id AS organization_id, p.id AS requester_user_id
  FROM organizations o
  JOIN profiles p ON p.email = 'org-standard@histar.vn'
  WHERE o.slug = 'demo-standard-org'
  LIMIT 1
)
INSERT INTO b2c_payment_transactions (
  id, user_id, provider, order_code, transfer_content, amount_vnd, status,
  return_to_path, expires_at, paid_at, created_at, updated_at, provider_gateway
)
SELECT
  gen_random_uuid(),
  payer.user_id,
  'SEPAY',
  'SEED-DASH-B2C-' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYY-MM'),
  'SEEDDASHB2C' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYYMM'),
  m.b2c_vnd,
  'PAID',
  '/billing',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days' + interval '10 hours',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '1 day',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days' + interval '10 hours',
  'SEED'
FROM months m
CROSS JOIN payer;

WITH months AS (
  SELECT * FROM (VALUES
    (5, 520000000, 350000000),
    (4, 610000000, 430000000),
    (3, 780000000, 560000000),
    (2, 690000000, 650000000),
    (1, 890000000, 760000000),
    (0, 1025000000, 820200000)
  ) AS t(offset_months, b2c_vnd, b2b_vnd)
),
org_edu AS (
  SELECT o.id AS organization_id, p.id AS requester_user_id, o.name AS org_name
  FROM organizations o
  JOIN profiles p ON p.email = 'org-standard@histar.vn'
  WHERE o.slug = 'demo-standard-org'
  LIMIT 1
),
org_museum AS (
  SELECT o.id AS organization_id,
         COALESCE(
           (SELECT id FROM profiles WHERE email = 'org-basic@histar.vn' LIMIT 1),
           (SELECT id FROM profiles WHERE email = 'org-standard@histar.vn' LIMIT 1)
         ) AS requester_user_id,
         o.name AS org_name
  FROM organizations o
  WHERE o.slug = 'cu-chi-demo'
  LIMIT 1
)
-- ~55% B2B as Edu (STANDARD), ~45% as Museum/Org (ORG_BASIC) for planRevenue donut.
INSERT INTO org_payment_transactions (
  id, requester_user_id, organization_id, org_name, contact_email, plan_type, provider,
  order_code, transfer_content, amount_vnd, status, return_to_path, expires_at, paid_at,
  created_at, updated_at, provider_gateway
)
SELECT
  gen_random_uuid(),
  CASE WHEN m.offset_months % 2 = 0 THEN edu.requester_user_id ELSE museum.requester_user_id END,
  CASE WHEN m.offset_months % 2 = 0 THEN edu.organization_id ELSE museum.organization_id END,
  CASE WHEN m.offset_months % 2 = 0 THEN edu.org_name ELSE museum.org_name END,
  CASE WHEN m.offset_months % 2 = 0 THEN 'org-standard@histar.vn' ELSE 'org-basic@histar.vn' END,
  CASE WHEN m.offset_months % 2 = 0 THEN 'STANDARD' ELSE 'ORG_BASIC' END,
  'SEPAY',
  'SEED-DASH-B2B-' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYY-MM'),
  'SEEDDASHB2B' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYYMM'),
  m.b2b_vnd,
  'PAID',
  '/billing',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '4 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days' + interval '14 hours',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days' + interval '14 hours',
  'SEED'
FROM months m
CROSS JOIN org_edu edu
CROSS JOIN org_museum museum;

-- Sample audit events for dashboard ops log (optional visual).
DELETE FROM admin_audit_events WHERE idempotency_key LIKE 'seed-dash-ops-%';

INSERT INTO admin_audit_events (
  id, actor_id, entity_type, entity_id, action, reason, metadata_json, occurred_at, idempotency_key
)
SELECT
  gen_random_uuid(),
  COALESCE(
    (SELECT id FROM profiles WHERE email IN ('admin@histar.vn', 'demo-admin@histar.vn') ORDER BY email LIMIT 1),
    (SELECT id FROM profiles WHERE email = 'premium@histar.vn' LIMIT 1)
  ),
  v.entity_type,
  v.entity_id::uuid,
  v.action,
  v.reason,
  v.metadata,
  now() - v.ago,
  v.idem
FROM (VALUES
  ('QUEST', 'a1111111-1111-4111-8111-111111111111', 'CREATED', 'Đơn hàng quest demo seed', '{"channel":"VietQR","amountVnd":380000}', interval '5 minutes', 'seed-dash-ops-1'),
  ('ORGANIZATION', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'UPDATED', 'Nạp credit đối tác B2B demo', '{"amountVnd":25000000}', interval '12 minutes', 'seed-dash-ops-2'),
  ('NARRATIVE_INVESTIGATION', 'c1111111-2222-4333-8444-555555555555', 'REVIEWED', 'Phê duyệt Pilot mới (seed)', '{"by":"admin"}', interval '45 minutes', 'seed-dash-ops-3'),
  ('DISCOVERY_POINT', 'd1111111-2222-4333-8444-666666666666', 'UPDATED', 'Khiếu nại sử liệu trạm #04', '{"status":"OPEN"}', interval '1 hour', 'seed-dash-ops-4'),
  ('ORGANIZATION', 'e1111111-2222-4333-8444-777777777777', 'CREATED', 'Tổ chức đăng ký mới (seed)', '{"name":"Bảo tàng Điêu khắc Chăm"}', interval '2 hours', 'seed-dash-ops-5')
) AS v(entity_type, entity_id, action, reason, metadata, ago, idem);

COMMIT;

-- END seed: 2026-10-15_admin_dashboard_revenue_demo.sql

-- ============================================================================
-- Flyway baseline after full bootstrap: V90
-- ============================================================================
CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM flyway_schema_history) THEN
        RAISE EXCEPTION 'Refusing to overwrite existing Flyway history. Use a new database.';
    END IF;
END $$;

INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script,
    checksum, installed_by, installed_on, execution_time, success
) VALUES (
    1, '90', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>',
    NULL, 'histar-full-bootstrap', NOW(), 0, TRUE
);

