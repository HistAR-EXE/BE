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
