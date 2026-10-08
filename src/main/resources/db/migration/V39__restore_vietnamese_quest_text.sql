-- Seed cũ bị ghi vào database bằng kết nối không phải UTF-8, dấu tiếng Việt thành dấu hỏi thật.
UPDATE locations
SET name = 'Địa đạo Củ Chi',
    description = 'Khu di tích lịch sử Địa đạo Củ Chi — hệ thống hầm ngầm thời kháng chiến tại huyện Củ Chi, TP.HCM.'
WHERE id = '11111111-1111-1111-1111-111111111111'
  AND (name LIKE '%?%' OR description LIKE '%?%');

UPDATE quests
SET title = 'Hành trình dưới lòng đất',
    description = 'Khám phá Bếp Hoàng Cầm, Phòng họp và Time Portal 1948 — sau đó check-in tại khu di tích Củ Chi.',
    story = 'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.'
WHERE id = '33333333-3333-3333-3333-333333333333'
  AND title LIKE '%?%';

UPDATE quests
SET title = 'Mật mã Sa bàn Tác chiến',
    description = 'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.',
    story = 'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".'
WHERE id = '33333333-3333-3333-3333-333333333334'
  AND title LIKE '%?%';

UPDATE quests
SET title = 'Ánh sáng Đèn Mù U',
    description = 'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.',
    story = 'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.'
WHERE id = '33333333-3333-3333-3333-333333333335'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 2 · Khảo sát Bẫy Chông',
    objective = 'Vào tour 360° tìm hiểu mô hình hầm chông tự tạo.',
    description = 'Những chiếc bẫy tre thô sơ nhưng là nỗi khiếp sợ của quân thù.',
    hint = 'Trong tour 360°, tìm điểm nóng "Khu vực Bẫy Chông".',
    action_label = 'Vào Tour 360°'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'tour:sa-ban'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 3 · Phục kích Xe Tăng',
    objective = 'Mở Time Portal xem cảnh phục kích xe tăng M41.',
    description = 'Tái hiện khoảnh khắc quân ta dùng chiến thuật du kích phá hủy xe tăng.',
    hint = 'Mở Portal chọn mốc thời gian 1968.',
    action_label = 'Mở Time Portal'
WHERE quest_id = '33333333-3333-3333-3333-333333333334'
  AND unlock_key = 'era:1968'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 2 · Trạm xá Dã chiến',
    objective = 'Tìm kiếm trạm y tế dưới lòng đất trong Tour 360°.',
    description = 'Nơi các bác sĩ quân y cứu chữa thương binh trong điều kiện thiếu thốn.',
    hint = 'Khám phá khu vực Trạm xá trong sa bàn 360°.',
    action_label = 'Vào Tour 360°'
WHERE quest_id = '33333333-3333-3333-3333-333333333335'
  AND unlock_key = 'tour:tram-xa'
  AND title LIKE '%?%';

UPDATE quest_steps
SET title = 'Chương 3 · Lớp học bình dân',
    objective = 'Mở Time Portal chứng kiến lớp học dưới ánh đèn mù u.',
    description = 'Sự kiên cường của những đứa trẻ học chữ dưới làn bom đạn.',
    hint = 'Mở Portal mốc 1968 tại khu vực Lớp học.',
    action_label = 'Mở Time Portal'
WHERE quest_id = '33333333-3333-3333-3333-333333333335'
  AND unlock_key = 'era:1968'
  AND title LIKE '%?%';
