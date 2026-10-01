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
