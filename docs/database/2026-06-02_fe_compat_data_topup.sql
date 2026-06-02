-- FE demo data top-up pack (idempotent-ish, PostgreSQL)
-- Goal: ensure key FE domains have enough data (>=10 records each)
-- Run after:
--   1) docs/database/TimeLens_DB_Schema.sql
--   2) docs/database/2026-06-02_fe_compat_migration.sql
--   3) docs/database/2026-06-02_fe_compat_indexes_seed.sql

BEGIN;

-- ============================================================
-- 1) Locations (target >= 10) + FE-friendly named records
-- ============================================================
INSERT INTO locations (name, description, city, latitude, longitude, cover_image, rating, is_ar_available, distance_km, created_at)
SELECT v.name, v.description, v.city, v.latitude, v.longitude, v.cover_image, v.rating, v.is_ar_available, NULL, now()
FROM (
    VALUES
        ('Dai Noi Hue', 'Khu di tich lich su vi dai nhat cua trieu dai nha Nguyen, mo khoa trai nghiem thuc te ao.', 'Hue', 16.463713, 107.590866, 'https://placehold.co/1200x800?text=Dai+Noi+Hue', 4.9, TRUE),
        ('Chua Thien Mu', 'Ngoi chua co kinh nam ben bo song Huong, bieu tuong tam linh cua co do.', 'Hue', 16.452395, 107.545135, 'https://placehold.co/1200x800?text=Chua+Thien+Mu', 4.8, FALSE),
        ('Hoang Thanh Thang Long', 'Trung tam quyen luc hoang cung qua nhieu trieu dai, noi bao ton dau an lich su Thang Long.', 'Ha Noi', 21.036619, 105.836342, 'https://placehold.co/1200x800?text=Hoang+Thanh+Thang+Long', 4.7, TRUE),
        ('Van Mieu Quoc Tu Giam', 'Truong dai hoc dau tien cua Viet Nam, noi ton vinh truyen thong hieu hoc.', 'Ha Noi', 21.028047, 105.835461, 'https://placehold.co/1200x800?text=Van+Mieu', 4.6, FALSE),
        ('Pho Co Hoi An', 'Do thi co ven song voi kien truc giao thoa Viet Hoa Nhat va di san van hoa the gioi.', 'Quang Nam', 15.880058, 108.338047, 'https://placehold.co/1200x800?text=Hoi+An', 4.8, TRUE),
        ('Thanh Nha Ho', 'Cong trinh da quy mo lon dau the ky 15, ky thuat xep da doc dao.', 'Thanh Hoa', 20.078847, 105.603346, 'https://placehold.co/1200x800?text=Thanh+Nha+Ho', 4.4, FALSE),
        ('Co Do Hoa Lu', 'Kinh do dau tien cua nha nuoc phong kien tap quyen Viet Nam.', 'Ninh Binh', 20.253238, 105.972155, 'https://placehold.co/1200x800?text=Hoa+Lu', 4.5, TRUE),
        ('Den Hung', 'Khu di tich tam linh quoc gia gan voi truyen thuyet cac vua Hung.', 'Phu Tho', 21.397382, 105.194107, 'https://placehold.co/1200x800?text=Den+Hung', 4.5, FALSE),
        ('Dia dao Cu Chi', 'He thong dia dao trong long dat gan lien voi lich su khang chien.', 'TP.HCM', 11.143000, 106.461000, 'https://placehold.co/1200x800?text=Cu+Chi', 4.6, TRUE),
        ('Ben Nha Rong', 'Noi gan voi hanh trinh ra di tim duong cuu nuoc cua Chu tich Ho Chi Minh.', 'TP.HCM', 10.772029, 106.704494, 'https://placehold.co/1200x800?text=Ben+Nha+Rong', 4.4, FALSE)
) AS v(name, description, city, latitude, longitude, cover_image, rating, is_ar_available)
WHERE NOT EXISTS (
    SELECT 1 FROM locations l WHERE lower(l.name) = lower(v.name)
);

WITH loc_count AS (
    SELECT COUNT(*) AS cnt FROM locations
),
need AS (
    SELECT GREATEST(0, 10 - cnt) AS n FROM loc_count
)
INSERT INTO locations (name, description, city, latitude, longitude, cover_image, rating, is_ar_available, distance_km, created_at)
SELECT
    'Demo Di Tich #' || gs.i,
    'Dia diem demo phuc vu FE show list khong can mock data #' || gs.i,
    CASE
        WHEN gs.i % 3 = 0 THEN 'Hue'
        WHEN gs.i % 2 = 0 THEN 'Ha Noi'
        ELSE 'TP.HCM'
    END,
    10.700000 + (gs.i * 0.015),
    106.500000 + (gs.i * 0.012),
    'https://placehold.co/1200x800?text=Demo+Location+' || gs.i,
    4.0 + ((gs.i % 10)::numeric / 10.0),
    (gs.i % 2 = 0),
    NULL,
    now()
FROM need
JOIN LATERAL generate_series(1, need.n) gs(i) ON TRUE;

UPDATE locations
SET
    cover_image = COALESCE(cover_image, 'https://placehold.co/1200x800?text=Location'),
    rating = COALESCE(rating, 0),
    is_ar_available = COALESCE(is_ar_available, FALSE);

-- ============================================================
-- 2) Profiles + leaderboard data (target >= 10)
-- ============================================================
WITH profile_count AS (
    SELECT COUNT(*) AS cnt FROM profiles
),
need AS (
    SELECT GREATEST(0, 12 - cnt) AS n FROM profile_count
)
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
SELECT
    'demo_user_' || gs.i || '@histar.vn',
    '$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe',
    'local',
    'USER',
    'Demo Explorer ' || gs.i,
    'https://placehold.co/400x400?text=User+' || gs.i,
    1 + (gs.i % 8),
    120 + gs.i * 70,
    CASE
        WHEN gs.i % 3 = 0 THEN 'Hue'
        WHEN gs.i % 2 = 0 THEN 'Ha Noi'
        ELSE 'TP.HCM'
    END,
    now() - (gs.i || ' days')::interval
FROM need
JOIN LATERAL generate_series(1, need.n) gs(i) ON TRUE
ON CONFLICT (email) DO NOTHING;

-- ============================================================
-- 3) Characters + conversations + messages (chat dataset)
-- ============================================================
WITH base AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM locations
),
chars_needed AS (
    SELECT b.id, b.rn
    FROM base b
    LEFT JOIN characters c ON c.location_id = b.id
    WHERE c.id IS NULL
)
INSERT INTO characters (location_id, name, era, persona_prompt, portrait_url)
SELECT
    c.location_id,
    'Nhan vat lich su #' || c.rn,
    'Thoi ky ' || (1900 + c.rn),
    'Ban la huong dan vien lich su cho dia diem #' || c.rn || '. Chi tra loi bang su kien co can cu, khong bia dat.',
    'https://placehold.co/800x800?text=Character+' || c.rn
FROM (
    SELECT id AS location_id, rn FROM chars_needed
) c;

WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM profiles LIMIT 10
),
chars AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn FROM characters LIMIT 10
)
INSERT INTO conversations (user_id, character_id, created_at)
SELECT
    u.id,
    c.id,
    now() - ((11 - u.rn) || ' days')::interval
FROM users u
JOIN chars c ON c.rn = u.rn
WHERE NOT EXISTS (
    SELECT 1 FROM conversations cv WHERE cv.user_id = u.id AND cv.character_id = c.id
);

WITH conv AS (
    SELECT id, created_at, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM conversations
    ORDER BY created_at, id
    LIMIT 10
)
INSERT INTO messages (conversation_id, role, content, created_at)
SELECT
    c.id,
    CASE WHEN gs.i % 2 = 1 THEN 'user' ELSE 'assistant' END,
    CASE
        WHEN gs.i % 2 = 1 THEN 'Cho toi them thong tin lich su cua dia diem nay.'
        ELSE 'Dia diem nay co gia tri lich su lon va la diem tham quan noi bat.'
    END,
    c.created_at + (gs.i || ' minutes')::interval
FROM conv c
JOIN generate_series(1, 2) gs(i) ON TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM messages m
    WHERE m.conversation_id = c.id
      AND m.created_at = c.created_at + (gs.i || ' minutes')::interval
);

-- ============================================================
-- 4) Quests + progress (target >= 10 quests and >=10 progress)
-- ============================================================
INSERT INTO quests (location_id, title, description, story, points_reward, required_order, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT l.id,
       'Dau an Hoang Thanh',
       'Giai ma cac co vat duoc tim thay tai khu vuc trung tam de khoi phuc dong thoi gian.',
       'Dong thoi gian tai Hoang Thanh dang roi loan, hay kham pha va khoi phuc.',
       500,
       1,
       4,
       1,
       'https://placehold.co/1200x800?text=Dau+an+Hoang+Thanh',
       'not_started',
       now()
FROM locations l
WHERE lower(l.name) = lower('Hoang Thanh Thang Long')
  AND NOT EXISTS (SELECT 1 FROM quests q WHERE lower(q.title) = lower('Dau an Hoang Thanh'));

INSERT INTO quests (location_id, title, description, story, points_reward, required_order, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT l.id,
       'Bi an Chua Cau',
       'Tim kiem cac dau vet thuong mai co dai doc theo bo song.',
       'Moi manh ghep ve thuong cang co xua deu dan toi bi an lon.',
       200,
       1,
       3,
       1,
       'https://placehold.co/1200x800?text=Bi+an+Chua+Cau',
       'not_started',
       now()
FROM locations l
WHERE lower(l.name) = lower('Pho Co Hoi An')
  AND NOT EXISTS (SELECT 1 FROM quests q WHERE lower(q.title) = lower('Bi an Chua Cau'));

INSERT INTO quests (location_id, title, description, story, points_reward, required_order, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT l.id,
       'Mat ma Lang Tam',
       'Giai ma van bia co de mo khoa mot manh lich su bi that lac.',
       'Chi nguoi choi dat cap do cao moi co the buoc tiep vao manh mo nay.',
       800,
       2,
       5,
       5,
       'https://placehold.co/1200x800?text=Mat+ma+Lang+Tam',
       'not_started',
       now()
FROM locations l
WHERE lower(l.name) = lower('Dai Noi Hue')
  AND NOT EXISTS (SELECT 1 FROM quests q WHERE lower(q.title) = lower('Mat ma Lang Tam'));

WITH q_count AS (
    SELECT COUNT(*) AS cnt FROM quests
),
need AS (
    SELECT GREATEST(0, 12 - cnt) AS n FROM q_count
),
loc AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM locations
)
INSERT INTO quests (location_id, title, description, story, points_reward, required_order, steps_total, unlock_level, cover_image, status_default, created_at)
SELECT
    l.id,
    'Nhiem vu demo #' || gs.i,
    'Nhiem vu duoc tao de FE co du lieu hien thi day du #' || gs.i,
    'Chuoi truyen demo cho quest #' || gs.i,
    100 + gs.i * 20,
    1,
    1 + (gs.i % 4),
    CASE WHEN gs.i % 5 = 0 THEN 5 ELSE 1 END,
    'https://placehold.co/1200x800?text=Quest+' || gs.i,
    'not_started',
    now()
FROM need
JOIN LATERAL generate_series(1, need.n) gs(i) ON TRUE
JOIN loc l ON l.rn = ((gs.i - 1) % (SELECT COUNT(*) FROM loc)) + 1;

WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    LIMIT 10
),
quests_rank AS (
    SELECT id, location_id, steps_total, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM quests
    LIMIT 10
)
INSERT INTO user_quest_progress (user_id, quest_id, location_id, status, current_step, steps_total, started_at, completed_at)
SELECT
    u.id,
    q.id,
    q.location_id,
    CASE
        WHEN u.rn <= 3 THEN 'completed'
        WHEN u.rn <= 7 THEN 'in_progress'
        ELSE 'not_started'
    END,
    CASE
        WHEN u.rn <= 3 THEN q.steps_total
        WHEN u.rn <= 7 THEN GREATEST(1, LEAST(q.steps_total, q.steps_total - 1))
        ELSE 0
    END,
    q.steps_total,
    now() - ((11 - u.rn) || ' days')::interval,
    CASE WHEN u.rn <= 3 THEN now() - ((8 - u.rn) || ' days')::interval ELSE NULL END
FROM users u
JOIN quests_rank q ON q.rn = u.rn
ON CONFLICT (user_id, quest_id) DO UPDATE
SET
    location_id = EXCLUDED.location_id,
    steps_total = EXCLUDED.steps_total,
    current_step = EXCLUDED.current_step;

-- Backfill missing location/steps for old rows
UPDATE user_quest_progress uqp
SET
    location_id = COALESCE(uqp.location_id, q.location_id),
    steps_total = COALESCE(NULLIF(uqp.steps_total, 0), COALESCE(q.steps_total, 1)),
    current_step = COALESCE(uqp.current_step, 0)
FROM quests q
WHERE q.id = uqp.quest_id;

-- ============================================================
-- 5) Badges + user badges (target >=10)
-- ============================================================
WITH b_count AS (
    SELECT COUNT(*) AS cnt FROM badges
),
need AS (
    SELECT GREATEST(0, 10 - cnt) AS n FROM b_count
)
INSERT INTO badges (name, description, icon_url, condition_type, condition_value)
SELECT
    'Huy hieu demo #' || gs.i,
    'Dieu kien demo cho badge #' || gs.i,
    'https://placehold.co/256x256?text=Badge+' || gs.i,
    CASE
        WHEN gs.i % 3 = 0 THEN 'points'
        WHEN gs.i % 2 = 0 THEN 'checkin'
        ELSE 'quest_complete'
    END,
    CASE
        WHEN gs.i % 3 = 0 THEN gs.i * 100
        ELSE gs.i
    END
FROM need
JOIN LATERAL generate_series(1, need.n) gs(i) ON TRUE;

WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM profiles LIMIT 10
),
badge_rank AS (
    SELECT id, row_number() OVER (ORDER BY id) AS rn FROM badges LIMIT 10
)
INSERT INTO user_badges (user_id, badge_id, earned_at)
SELECT
    u.id,
    b.id,
    now() - ((10 - u.rn) || ' days')::interval
FROM users u
JOIN badge_rank b ON b.rn = u.rn
ON CONFLICT (user_id, badge_id) DO NOTHING;

-- ============================================================
-- 6) Checkins + creations + secret unlocks (target >=10)
-- ============================================================
WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM profiles LIMIT 10
),
loc AS (
    SELECT id, latitude, longitude, row_number() OVER (ORDER BY created_at, id) AS rn FROM locations LIMIT 10
)
INSERT INTO checkins (user_id, location_id, latitude, longitude, created_at)
SELECT
    u.id,
    l.id,
    COALESCE(l.latitude, 10.0) + 0.0002,
    COALESCE(l.longitude, 106.0) + 0.0002,
    now() - ((11 - u.rn) || ' days')::interval
FROM users u
JOIN loc l ON l.rn = u.rn
WHERE NOT EXISTS (
    SELECT 1 FROM checkins c
    WHERE c.user_id = u.id
      AND c.location_id = l.id
);

WITH frame_rank AS (
    SELECT id, row_number() OVER (ORDER BY sort_order, id) AS rn
    FROM photo_frames
    LIMIT 10
),
users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn
    FROM profiles
    LIMIT 10
)
INSERT INTO user_creations (user_id, frame_id, output_url, variant, shared_at, created_at)
SELECT
    u.id,
    f.id,
    'https://placehold.co/1080x1080?text=Creation+' || u.rn,
    CASE WHEN u.rn % 2 = 0 THEN 'story' ELSE 'square' END,
    CASE WHEN u.rn <= 5 THEN now() - ((6 - u.rn) || ' days')::interval ELSE NULL END,
    now() - ((8 - u.rn) || ' days')::interval
FROM users u
JOIN frame_rank f ON f.rn = u.rn
WHERE NOT EXISTS (
    SELECT 1 FROM user_creations uc
    WHERE uc.user_id = u.id
      AND uc.frame_id = f.id
);

WITH users AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM profiles LIMIT 10
),
loc AS (
    SELECT id, row_number() OVER (ORDER BY created_at, id) AS rn FROM locations LIMIT 10
)
INSERT INTO user_secret_unlocks (user_id, location_id, unlocked_at)
SELECT
    u.id,
    l.id,
    now() - ((9 - u.rn) || ' days')::interval
FROM users u
JOIN loc l ON l.rn = u.rn
ON CONFLICT (user_id, location_id) DO NOTHING;

COMMIT;
