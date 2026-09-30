-- V32: Align story_chapters + station_minigames with MVBP doc §5 / V28 station names.
-- ST01 Phòng họp → ST02 Kho ngầm → ST03 Giếng hầm → ST04 Quân y → ST05 Xưởng vũ khí → ST06 Hầm phòng thủ

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 1: Rạng sáng ở Phòng họp',
    synopsis = 'Ánh đèn dầu leo lét. Bạn chứng kiến cuộc họp bí mật dưới lòng đất: kế hoạch của ngày, mật hiệu và nhiệm vụ liên lạc. Cliffhanger: khẩu hiệu mở kho ngầm là gì?'
WHERE site_code = 'cu-chi' AND chapter_number = 1 AND station_code = 'ST01';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 2: Nuôi quân trong im lặng',
    synopsis = 'Kho ngầm và bếp Hoàng Cầm (nếu trên tuyến): lương khô, giấu khói, nuôi quân không lộ vị trí. Cliffhanger: ống dẫn khói dẫn đi đâu?'
WHERE site_code = 'cu-chi' AND chapter_number = 2 AND station_code = 'ST02';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 3: Nước và hơi thở',
    synopsis = 'Giếng hầm và lỗ thông hơi — nguồn sống dưới địa đạo. Bạn học cách lấy nước và ngụy trang miệng lỗ. Cliffhanger: đâu là lỗ thông hơi thật?'
WHERE site_code = 'cu-chi' AND chapter_number = 3 AND station_code = 'ST03';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 4: Sơ cứu dưới ánh đèn dầu',
    synopsis = 'Quân y: chăm sóc người bị thương với thảo dược và dụng cụ thiếu thốn. Cliffhanger: thứ tự sơ cứu đúng theo tư liệu là gì?'
WHERE site_code = 'cu-chi' AND chapter_number = 4 AND station_code = 'ST04';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 5: Tái chế từ đồng nát',
    synopsis = 'Xưởng vũ khí ngầm: biến vật liệu phế thành công cụ, nhấn mạnh sáng tạo và an toàn lao động. Cliffhanger: mảnh nào ghép thành dụng cụ?'
WHERE site_code = 'cu-chi' AND chapter_number = 5 AND station_code = 'ST05';

UPDATE story_chapters SET
    title = 'Một ngày dưới lòng đất – Chương 6: Lối thoát an toàn',
    synopsis = 'Hầm phòng thủ nhiều tầng: trực gác, lỗ quan sát và cửa thoát khi bị vây. Kết thúc một ngày dưới lòng đất. Cliffhanger: tuyến nào an toàn nhất?'
WHERE site_code = 'cu-chi' AND chapter_number = 6 AND station_code = 'ST06';

-- Mini-games: titles + config aligned to V28 station themes (doc §4 B6)
UPDATE station_minigames SET
    title = 'Giải mã mật hiệu — Phòng họp',
    game_type = 'MATCH_TERMS',
    config_json = '{"terms":["Phòng họp dưới đất","Ánh đèn dầu","Kế hoạch ngày","Liên lạc viên"],"definitions":["Nơi chỉ huy bàn kế hoạch trong địa đạo","Ánh sáng hạn chế, tiết kiệm nhiên liệu","Được thống nhất trước khi xuất kích","Người truyền tin giữa các tổ"],"answerKey":[0,1,2,3]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST01';

UPDATE station_minigames SET
    title = 'Khói đi đâu? — Kho ngầm & bếp',
    game_type = 'TIMELINE_ORDER',
    config_json = '{"items":["Chuẩn bị củi và nồi","Đào / kiểm ống dẫn khói","Nấu trong đêm im lặng","Che kín miệng hầm khói"],"hint":"Theo trình tự nấu ăn bí mật dưới địa đạo (bếp Hoàng Cầm)."}'
WHERE site_code = 'cu-chi' AND station_code = 'ST02';

UPDATE station_minigames SET
    title = 'Tìm lỗ thông hơi — Giếng hầm',
    game_type = 'SPOT_DIFF',
    config_json = '{"leftTitle":"Bề mặt ngụy trang","rightTitle":"Chi tiết thật","leftItems":["Bụi cây che miệng lỗ","Lỗ thông hơi lộ rõ","Nắp giếng kín","Cỏ khô rải quanh"],"rightItems":["Bụi cây che miệng lỗ","Lỗ thông hơi được ngụy trang","Nắp giếng kín","Cỏ khô rải quanh"],"diffIndices":[1]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST03';

UPDATE station_minigames SET
    title = 'Sơ cứu dưới ánh đèn dầu — Quân y',
    game_type = 'TIMELINE_ORDER',
    config_json = '{"items":["Kiểm tra đường thở","Cầm máu bằng băng gạc","Đắp thảo dược đã duyệt","Chuyển người bị thương an toàn"],"hint":"Thứ tự sơ cứu theo tư liệu quân y địa đạo (đã duyệt)."}'
WHERE site_code = 'cu-chi' AND station_code = 'ST04';

UPDATE station_minigames SET
    title = 'Tái chế từ đồng nát — Xưởng vũ khí',
    game_type = 'MEMORY_PAIRS',
    config_json = '{"pairs":[{"id":"a","label":"Phôi kim loại"},{"id":"b","label":"Khuôn"},{"id":"c","label":"Búa thợ"},{"id":"d","label":"Dây buộc"},{"id":"e","label":"Đèn dầu xưởng"},{"id":"f","label":"Hộp dụng cụ"}]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST05';

UPDATE station_minigames SET
    title = 'Lối thoát an toàn — Hầm phòng thủ',
    game_type = 'COMPASS_CHOICE',
    config_json = '{"scenario":"Hầm bị vây. Bạn cần chọn tuyến thoát an toàn theo sơ đồ nhiều tầng.","choices":[{"id":"n","label":"Hầm chính đã trinh sát","correct":true},{"id":"e","label":"Lối đông đang có tuần tra","correct":false},{"id":"s","label":"Lối tắt chưa kiểm tra","correct":false},{"id":"w","label":"Mở ra vùng trống dễ lộ","correct":false}]}'
WHERE site_code = 'cu-chi' AND station_code = 'ST06';
