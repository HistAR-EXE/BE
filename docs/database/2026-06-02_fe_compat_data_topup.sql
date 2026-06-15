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
