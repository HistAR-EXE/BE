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
