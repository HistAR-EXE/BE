-- The current 360 tour is a third-party iframe: TimeLens cannot observe which
-- scene was visited. Replace scene-dependent quest progress with first-party
-- interactions. Keep quest/step IDs and all historical user progress intact.

-- These three Premium quests formerly waited for two panorama scene keys that
-- the embedded tour cannot report. Their own verified dialogue/minigames remain.
UPDATE quests
SET unlock_discovery_keys = NULL
WHERE id IN (
    '33333333-3333-3333-3333-333333333337'::uuid,
    '33333333-3333-3333-3333-333333333338'::uuid,
    '33333333-3333-3333-3333-333333333339'::uuid
);

UPDATE quests
SET description = 'So sánh các dấu tích Củ Chi xưa và nay tại Bếp Hoàng Cầm, Phòng họp và Lỗ thông hơi. Chương 1948 là bước tùy chọn Premium.',
    step_discovery_keys = 'xuanay:phong-hop,xuanay:thong-hoi,xuanay:bep-hoang-cam'
WHERE id = '33333333-3333-3333-3333-333333333333'::uuid;

UPDATE quest_steps
SET unlock_key = 'xuanay:phong-hop',
    title = 'Xưa & Nay · Phòng họp',
    objective = 'Tự kéo thanh so sánh Phòng họp xưa và nay rồi ở lại ít nhất 4 giây.',
    description = 'Xem ảnh tái hiện và ảnh hiện nay trong Cổng thời gian.',
    hint = 'Cổng thời gian → Xưa & Nay → Phòng họp.',
    action_type = 'xuanay',
    action_label = 'So sánh Phòng họp'
WHERE id = '33333333-3333-3333-3333-333333333301'::uuid
  AND unlock_key = 'scene:22222222-2222-2222-2222-222222222221';

UPDATE quest_steps
SET unlock_key = 'xuanay:thong-hoi',
    title = 'Xưa & Nay · Lỗ thông hơi',
    objective = 'Tự kéo thanh so sánh Lỗ thông hơi xưa và nay rồi ở lại ít nhất 4 giây.',
    description = 'Quan sát cách lỗ thông hơi được ngụy trang.',
    hint = 'Cổng thời gian → Xưa & Nay → Lỗ thông hơi.',
    action_type = 'xuanay',
    action_label = 'So sánh Lỗ thông hơi'
WHERE id = '33333333-3333-3333-3333-333333333302'::uuid
  AND unlock_key = 'scene:22222222-2222-2222-2222-222222222223';

-- The Sa bàn question already exists in Chat. A correct answer now records
-- tour:sa-ban on the server, so the quest is completable without the old map.
UPDATE quest_steps
SET title = 'Giải câu hỏi Sa bàn',
    objective = 'Trả lời câu hỏi về độ sâu tầng sâu nhất của địa đạo.',
    description = 'Mở câu hỏi trong TimeLens; đáp án đúng được máy chủ ghi nhận.',
    hint = 'Chọn đáp án về tầng sâu nhất trong cửa sổ câu hỏi.',
    action_type = 'quiz',
    action_label = 'Giải câu hỏi Sa bàn'
WHERE id = '33333333-3333-3333-3333-333333333342'::uuid
  AND unlock_key = 'tour:sa-ban';

UPDATE discovery_points
SET name = 'Giải câu hỏi Sa bàn'
WHERE location_id = '11111111-1111-1111-1111-111111111111'::uuid
  AND unlock_key = 'tour:sa-ban'
  AND name = 'Mảnh sa bàn';

UPDATE quests
SET description = 'Hỏi Chrono về cuộc sống dưới lòng đất rồi so sánh ánh sáng trong hầm xưa và nay.',
    step_discovery_keys = 'dialogue:chi-nam,xuanay:den-ham,era:1968'
WHERE id = '33333333-3333-3333-3333-333333333335'::uuid;

UPDATE quest_steps
SET unlock_key = 'xuanay:den-ham',
    title = 'Xưa & Nay · Ánh sáng trong hầm',
    objective = 'Tự kéo thanh so sánh ánh sáng trong hầm xưa và nay rồi ở lại ít nhất 4 giây.',
    description = 'Tìm hiểu ánh sáng sinh hoạt dưới lòng đất qua hai thời điểm.',
    hint = 'Cổng thời gian → Xưa & Nay → Ánh sáng trong hầm.',
    action_type = 'xuanay',
    action_label = 'So sánh ánh sáng'
WHERE id = '33333333-3333-3333-3333-333333333352'::uuid
  AND unlock_key = 'tour:tram-xa';

-- Keep historical discovery bindings, but do not associate their obsolete
-- scene/tour keys with the newly defined first-party quest steps.
UPDATE discovery_content_bindings
SET quest_step_id = NULL
WHERE quest_step_id IN (
    '33333333-3333-3333-3333-333333333301'::uuid,
    '33333333-3333-3333-3333-333333333302'::uuid,
    '33333333-3333-3333-3333-333333333352'::uuid
)
AND unlock_key IN (
    'scene:22222222-2222-2222-2222-222222222221',
    'scene:22222222-2222-2222-2222-222222222223',
    'tour:tram-xa'
);

-- Carry forward already earned step progress under the replacement keys.
-- Keep the original rows as historical discovery records.
INSERT INTO user_discoveries (user_id, location_id, discovery_key, discovered_at)
SELECT d.user_id, d.location_id, v.new_key, d.discovered_at
FROM user_discoveries d
JOIN (VALUES
    ('scene:22222222-2222-2222-2222-222222222221', 'xuanay:phong-hop'),
    ('scene:22222222-2222-2222-2222-222222222223', 'xuanay:thong-hoi'),
    ('tour:tram-xa', 'xuanay:den-ham')
) AS v(old_key, new_key) ON d.discovery_key = v.old_key
WHERE d.location_id = '11111111-1111-1111-1111-111111111111'::uuid
ON CONFLICT (user_id, location_id, discovery_key) DO NOTHING;

-- Online artifact quests at the other nine sites already have artifact rows and
-- step keys, but no matching discovery_points. Without these rows the record
-- endpoint rejects the view after the three-second timer, so none can finish.
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
SELECT gen_random_uuid(), q.location_id, COALESCE(NULLIF(s.title, ''), 'Hiện vật'),
       50.00, 50.00, s.unlock_key, 500 + COALESCE(s.step_order, 0)
FROM quest_steps s
JOIN quests q ON q.id = s.quest_id
WHERE q.track = 'ONLINE'
  AND q.deleted_at IS NULL
  AND s.action_type = 'artifact'
  AND s.unlock_key LIKE 'artifact:%'
  AND q.id::text LIKE '44444444-4444-4444-4444-4444444444%'
ON CONFLICT (location_id, unlock_key) DO NOTHING;
