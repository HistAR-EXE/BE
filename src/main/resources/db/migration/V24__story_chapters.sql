-- B4: Story chapters mapped to stations; chapters 1-2 free, 3+ require Premium.
CREATE TABLE IF NOT EXISTS story_chapters (
    id               UUID PRIMARY KEY,
    site_code        VARCHAR(64)  NOT NULL DEFAULT 'cu-chi',
    chapter_number   INT          NOT NULL,
    station_code     VARCHAR(32)  NOT NULL,
    title            VARCHAR(255) NOT NULL,
    synopsis         TEXT,
    requires_premium BOOLEAN      NOT NULL DEFAULT FALSE,
    sort_order       INT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_story_chapters_site_number UNIQUE (site_code, chapter_number),
    CONSTRAINT fk_story_chapters_station FOREIGN KEY (site_code, station_code)
        REFERENCES stations (site_code, code)
);

CREATE INDEX IF NOT EXISTS idx_story_chapters_site_sort
    ON story_chapters (site_code, sort_order);

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a4000001-0000-4000-8000-000000000001', 'cu-chi', 1, 'ST01',
 'Một ngày dưới lòng đất – Chương 1: Bến Dược thức giấc',
 'Bình minh trên Bến Dược, nơi cuộc hành trình một ngày dưới lòng đất bắt đầu. Bạn nhận nhiệm vụ đầu tiên từ người liên lạc và bước vào vùng đất thép.',
 FALSE, 1),
('a4000001-0000-4000-8000-000000000002', 'cu-chi', 2, 'ST02',
 'Một ngày dưới lòng đất – Chương 2: Bước xuống địa đạo',
 'Cửa hầm hé mở. Bạn cúi người chui xuống mạng lưới địa đạo chằng chịt, làm quen với bóng tối, hơi đất và nhịp thở của những người đã sống ở đây.',
 FALSE, 2),
('a4000001-0000-4000-8000-000000000003', 'cu-chi', 3, 'ST03',
 'Một ngày dưới lòng đất – Chương 3: Bếp Hoàng Cầm không khói',
 'Bữa cơm giữa chiến trường: bí mật của chiếc bếp không khói và những đêm nấu ăn lặng lẽ để không lộ vị trí.',
 TRUE, 3),
('a4000001-0000-4000-8000-000000000004', 'cu-chi', 4, 'ST04',
 'Một ngày dưới lòng đất – Chương 4: Giọt nước giữa chiến hào',
 'Giếng nước – nguồn sống của cả khu căn cứ. Bạn theo chân người tải nước trong đêm và hiểu vì sao mỗi giọt nước đều quý.',
 TRUE, 4),
('a4000001-0000-4000-8000-000000000005', 'cu-chi', 5, 'ST05',
 'Một ngày dưới lòng đất – Chương 5: Ánh đèn phòng họp',
 'Dưới ánh đèn dầu leo lét, các chỉ huy vạch kế hoạch cho trận đánh. Bạn được chứng kiến một cuộc họp quyết định.',
 TRUE, 5),
('a4000001-0000-4000-8000-000000000006', 'cu-chi', 6, 'ST06',
 'Một ngày dưới lòng đất – Chương 6: Xuất phát',
 'Kết thúc một ngày dưới lòng đất, đội hình xuất phát trong đêm. Bạn trở lại mặt đất mang theo câu chuyện của Củ Chi.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;
