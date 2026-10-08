INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000030', '11111111-1111-1111-1111-111111111111', 'Hỏi Chrono về các tầng', 30.00, 35.00, 'dialogue:anh-ba', 30),
('d0000001-0000-4000-8000-000000000031', '11111111-1111-1111-1111-111111111111', 'Hỏi Chrono về bếp Hoàng Cầm', 30.00, 35.00, 'dialogue:chi-nam', 31),
('d0000001-0000-4000-8000-000000000032', '11111111-1111-1111-1111-111111111111', 'Mảnh sa bàn', 36.00, 42.00, 'tour:sa-ban', 32),
('d0000001-0000-4000-8000-000000000033', '11111111-1111-1111-1111-111111111111', 'Tour trạm xá', 50.00, 50.00, 'tour:tram-xa', 33)
ON CONFLICT (location_id, unlock_key) DO NOTHING;

UPDATE quest_steps
SET objective = 'Tìm mảnh giấu gần nhà sa bàn trên sơ đồ, rồi đưa mã cho Chrono.',
    description = 'Đứng trong tour không đủ. Mảnh nằm gần nhà biểu diễn sa bàn.',
    hint = 'Tìm mảnh gần nhà sa bàn, dán mã cho Chrono.',
    action_label = 'Tìm mảnh trên sơ đồ'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'tour:sa-ban';
