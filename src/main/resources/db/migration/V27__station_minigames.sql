-- B6: Station mini-games (one per ST01–ST06) and per-user scores.
CREATE TABLE IF NOT EXISTS station_minigames (
    id            UUID PRIMARY KEY,
    site_code     VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    station_code  VARCHAR(32)  NOT NULL,
    game_type     VARCHAR(32)  NOT NULL,
    title         VARCHAR(255) NOT NULL,
    config_json   TEXT         NOT NULL,
    sort_order    INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_station_minigames_site_station UNIQUE (site_code, station_code),
    CONSTRAINT fk_station_minigames_station FOREIGN KEY (site_code, station_code)
        REFERENCES stations (site_code, code)
);

CREATE INDEX IF NOT EXISTS idx_station_minigames_site_station
    ON station_minigames (site_code, station_code);

CREATE TABLE IF NOT EXISTS station_minigame_scores (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    minigame_id   UUID         NOT NULL REFERENCES station_minigames (id) ON DELETE CASCADE,
    score         INT          NOT NULL,
    completed_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_station_minigame_scores_user_game UNIQUE (user_id, minigame_id)
);

CREATE INDEX IF NOT EXISTS idx_station_minigame_scores_user
    ON station_minigame_scores (user_id);

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b6000001-0000-4000-8000-000000000001', 'cu-chi', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm nhanh: Bến Dược',
 '{"timeLimitSec":90,"questions":[{"prompt":"Bến Dược nổi tiếng vì điều gì?","options":["Là cửa ngõ vào vùng đất thép","Là bãi biển du lịch","Là nhà máy xi măng"],"correctIndex":0},{"prompt":"Nhiệm vụ đầu tiên của du kích thường là gì?","options":["Nhận chỉ thị và chuẩn bị hành trang","Mua vé tham quan","Chụp ảnh selfie"],"correctIndex":0},{"prompt":"Cuộc sống vùng Củ Chi thời kháng chiến được mô tả thế nào?","options":["Gắn bó, kiên cường","Chỉ có du lịch","Hoàn toàn yên bình như thành phố"],"correctIndex":0}]}',
 1),
('b6000001-0000-4000-8000-000000000002', 'cu-chi', 'ST02', 'MEMORY_PAIRS',
 'Ghép cặp: Địa đạo Củ Chi',
 '{"pairs":[{"id":"a","label":"Miệng hầm"},{"id":"b","label":"Hầm đi bộ"},{"id":"c","label":"Hầm ngầm"},{"id":"d","label":"Bẫy địa đạo"},{"id":"e","label":"Lối thoát hiểm"},{"id":"f","label":"Phòng y tế"}]}',
 2),
('b6000001-0000-4000-8000-000000000003', 'cu-chi', 'ST03', 'TIMELINE_ORDER',
 'Sắp thứ tự: Bếp Hoàng Cầm',
 '{"items":["Chuẩn bị củi và nồi","Đào hầm thoát khói","Nấu cơm trong đêm","Che kín miệng hầm khói"],"hint":"Theo trình tự nấu ăn bí mật dưới địa đạo."}',
 3),
('b6000001-0000-4000-8000-000000000004', 'cu-chi', 'ST04', 'SPOT_DIFF',
 'Tìm khác biệt: Giếng nước',
 '{"leftTitle":"Ban ngày (an toàn)","rightTitle":"Ban đêm (bí mật)","leftItems":["Giếng có nắp che","Có người canh gác rõ ràng","Xô nước để cạnh giếng","Tiếng kéo nước vang"],"rightItems":["Giếng có nắp che","Không có người canh — lấy nước lén","Xô nước để cạnh giếng","Tiếng kéo nước được hạn chế"],"diffIndices":[1,3]}',
 4),
('b6000001-0000-4000-8000-000000000005', 'cu-chi', 'ST05', 'MATCH_TERMS',
 'Nối thuật ngữ: Phòng họp',
 '{"terms":["Phòng họp dưới đất","Ánh đèn dầu","Kế hoạch tấn công","Liên lạc viên"],"definitions":["Nơi chỉ huy bàn kế hoạch trong địa đạo","Ánh sáng hạn chế, tiết kiệm nhiên liệu","Được thống nhất trước khi xuất kích","Người truyền tin giữa các tổ"],"answerKey":[0,1,2,3]}',
 5),
('b6000001-0000-4000-8000-000000000006', 'cu-chi', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: Xuất phát đêm',
 '{"scenario":"Đội hình sắp rời căn cứ dưới lòng đất. Bạn cần chọn hướng di chuyển an toàn về khu vực Bến Dược.","choices":[{"id":"n","label":"Bắc — theo hầm chính đã được trinh sát","correct":true},{"id":"e","label":"Đông — qua khu đang có tuần tra địch","correct":false},{"id":"s","label":"Nam — lối tắt chưa được kiểm tra","correct":false},{"id":"w","label":"Tây — hướng mở ra vùng trống dễ lộ","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;
