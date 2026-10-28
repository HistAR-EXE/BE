CREATE TABLE explore_video_progress (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    video_id varchar(64) NOT NULL,
    watched_seconds double precision NOT NULL DEFAULT 0,
    session_id uuid,
    heartbeat_at timestamptz,
    updated_at timestamptz NOT NULL,
    UNIQUE(user_id, video_id),
    CONSTRAINT explore_video_watched_nonnegative CHECK (watched_seconds >= 0)
);

INSERT INTO discovery_points(id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
('d0000001-0000-4000-8000-000000000301','11111111-1111-1111-1111-111111111111','Video: Địa đạo Củ Chi đã được đào như thế nào',30,30,'video:tunnel-digging',301),
('d0000001-0000-4000-8000-000000000302','11111111-1111-1111-1111-111111111111','Video: Cổng làng vùng giải phóng',30,30,'video:liberation-gate',302),
('d0000001-0000-4000-8000-000000000303','11111111-1111-1111-1111-111111111111','Video: Trạm thông tin',30,30,'video:communication-station',303),
('d0000001-0000-4000-8000-000000000304','11111111-1111-1111-1111-111111111111','Video: Nhà du kích',30,30,'video:guerrilla-house',304),
('d0000001-0000-4000-8000-000000000305','11111111-1111-1111-1111-111111111111','Video: Nhà phụ nữ',30,30,'video:women-house',305),
('d0000001-0000-4000-8000-000000000306','11111111-1111-1111-1111-111111111111','Video: Nhà trưng bày hiện vật chiến tranh',30,30,'video:war-artifacts',306),
('d0000001-0000-4000-8000-000000000307','11111111-1111-1111-1111-111111111111','Video: Không gian văn hóa và tượng đài Chủ tịch Hồ Chí Minh',30,30,'video:ho-chi-minh-space',307)
ON CONFLICT (id) DO NOTHING;

-- Earned user_badges are deliberately retained. Updated rules apply to future awards.
UPDATE badges SET description = 'Hoàn thành sáu hồ sơ và bảy video Củ Chi (xem tối thiểu 90% và trả lời đúng thử thách).'
WHERE id = 'b0000001-0000-4000-8000-000000000207';
UPDATE badges SET description = '1.340 XP toàn tài khoản, hoàn thành bảy video Củ Chi, trải nghiệm 360°, sa bàn, AI và mở khóa cổ vật.'
WHERE id = 'b0000001-0000-4000-8000-000000000209';
