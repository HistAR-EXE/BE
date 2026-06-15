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
