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
  11.143, 106.461, 'TP.HCM',
  'https://placehold.co/800x500?text=Cu+Chi'
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
