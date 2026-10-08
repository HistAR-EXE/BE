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
