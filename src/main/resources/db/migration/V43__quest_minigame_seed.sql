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
