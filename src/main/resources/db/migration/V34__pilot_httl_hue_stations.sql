-- V34: Pilot sites — Hoàng thành Thăng Long + Đại Nội Huế (same format as Cu Chi ST01–ST06).
-- Station names follow the approved 3-region draft; story/minigame copy based on public heritage facts
-- (UNESCO / BQL materials). Replace via Admin CMS after BQL legal sign-off if needed.

-- ========== Hoàng thành Thăng Long ==========
INSERT INTO stations (id, site_code, code, name, sort_order, lat, lng, quest_step_key, is_active) VALUES
('a3400001-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 'ST01', 'Cổng tiếp nhận Hoàng Diệu', 1, 21.036578, 105.817288, 'httl:ST01', TRUE),
('a3400001-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 'ST02', 'Khu khảo cổ 18 Hoàng Diệu', 2, 21.037100, 105.834800, 'httl:ST02', TRUE),
('a3400001-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 'ST03', 'Nền Điện Kính Thiên', 3, 21.038200, 105.835200, 'httl:ST03', TRUE),
('a3400001-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 'ST04', 'Bảo tàng Hoàng thành', 4, 21.037800, 105.834500, 'httl:ST04', TRUE),
('a3400001-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 'ST05', 'Hầm chỉ huy', 5, 21.036900, 105.833900, 'httl:ST05', TRUE),
('a3400001-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 'ST06', 'Kỳ đài — Cột cờ Hà Nội', 6, 21.039000, 105.834000, 'httl:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3400002-0000-4000-8000-000000000001', 'a3400001-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu',
 'Hoàng thành Thăng Long là Di sản Thế giới UNESCO — trung tâm chính trị suốt hơn một thiên niên kỷ. Trạm mở đầu giúp bạn định hướng hành trình 6 điểm.', 1),
('a3400002-0000-4000-8000-000000000002', 'a3400001-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu',
 'Khu khảo cổ 18 Hoàng Diệu lộ các tầng văn hóa Lý — Trần — Lê. Móng cột và dấu vết kiến trúc cho thấy sự liên tục của kinh đô.', 1),
('a3400002-0000-4000-8000-000000000003', 'a3400001-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu',
 'Điện Kính Thiên từng là hạt nhân hoàng cung. Ngày nay còn nền đá và đôi rồng đá thời Lê Sơ — biểu tượng quyền lực và thẩm mỹ cung đình.', 1),
('a3400002-0000-4000-8000-000000000004', 'a3400001-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu',
 'Trưng bày ngói men, gốm sứ ngự dụng và hiện vật khai quật — cầu nối giữa khảo cổ học và câu chuyện cung đình.', 1),
('a3400002-0000-4000-8000-000000000005', 'a3400001-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu',
 'Hầm chỉ huy trong khu Hoàng thành gắn với lịch sử kháng chiến hiện đại — lớp ký ức chồng lên di sản cổ.', 1),
('a3400002-0000-4000-8000-000000000006', 'a3400001-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu',
 'Kỳ đài (Cột cờ Hà Nội) xây 1812 thời Gia Long, cao gần 33m — công trình hiếm còn nguyên qua thuộc địa và chiến tranh.', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a3400003-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 1, 'ST01',
 'Một ngày ở Hoàng thành — Chương 1: Cổng Hoàng Diệu',
 'Bạn bước vào khu di sản UNESCO, nhận nhiệm vụ khám phá sáu trạm và hiểu vì sao Thăng Long là trái tim chính trị của Đại Việt.',
 FALSE, 1),
('a3400003-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 2, 'ST02',
 'Một ngày ở Hoàng thành — Chương 2: Tầng đất 18 Hoàng Diệu',
 'Xuống khu khảo cổ: từng lớp đất kể về Lý, Trần, Lê. Bạn học đọc dấu vết móng cột như đọc một cuốn sử dưới chân.',
 FALSE, 2),
('a3400003-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 3, 'ST03',
 'Một ngày ở Hoàng thành — Chương 3: Đôi rồng đá Kính Thiên',
 'Trước nền Điện Kính Thiên, đôi rồng đá thế kỷ XV kể về nghi thức triều đình và thẩm mỹ thời Lê.',
 TRUE, 3),
('a3400003-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 4, 'ST04',
 'Một ngày ở Hoàng thành — Chương 4: Ngói men và bát ngự',
 'Trong bảo tàng, ngói chim phượng và gốm sứ ngự dụng hé lộ đời sống cung đình và kỹ nghệ lò quan.',
 TRUE, 4),
('a3400003-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 5, 'ST05',
 'Một ngày ở Hoàng thành — Chương 5: Lớp ký ức dưới hầm',
 'Hầm chỉ huy chồng lên di sản cổ — bạn đối chiếu hai thời kỳ trong cùng một không gian.',
 TRUE, 5),
('a3400003-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 6, 'ST06',
 'Một ngày ở Hoàng thành — Chương 6: Ngọn cờ trên Kỳ đài',
 'Kết thúc hành trình tại Cột cờ Hà Nội — biểu tượng sống sót qua biến thiên lịch sử.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b3400001-0000-4000-8000-000000000001', 'hoang-thanh-thang-long', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm: Hoàng thành Thăng Long',
 '{"timeLimitSec":90,"questions":[{"prompt":"Hoàng thành Thăng Long được UNESCO công nhận là gì?","options":["Di sản Thế giới","Khu vui chơi","Bảo tàng tư nhân"],"correctIndex":0},{"prompt":"Địa chỉ chính thức gần với đường nào?","options":["Hoàng Diệu, Ba Đình","Nguyễn Huệ, Q.1","Lê Lợi, Huế"],"correctIndex":0},{"prompt":"Thăng Long từng là gì trong lịch sử?","options":["Kinh đô nhiều triều đại","Chỉ một ngôi chùa","Cảng biển"],"correctIndex":0}]}',
 1),
('b3400001-0000-4000-8000-000000000002', 'hoang-thanh-thang-long', 'ST02', 'TIMELINE_ORDER',
 'Sắp thứ tự: tầng văn hóa',
 '{"items":["Lớp văn hóa thời Lý","Lớp văn hóa thời Trần","Lớp văn hóa thời Lê","Lớp khảo cổ hiện đại"],"hint":"Từ sớm đến muộn theo dòng lịch sử kinh đô."}',
 2),
('b3400001-0000-4000-8000-000000000003', 'hoang-thanh-thang-long', 'ST03', 'MATCH_TERMS',
 'Nối thuật ngữ: Điện Kính Thiên',
 '{"terms":["Điện Kính Thiên","Đôi rồng đá","Nền điện","Lê Sơ"],"definitions":["Hạt nhân trung tâm hoàng cung xưa","Kiệt tác điêu khắc thế kỷ XV còn lại","Phần kiến trúc còn thấy trên mặt đất","Triều đại gắn với đôi rồng đá"],"answerKey":[0,1,2,3]}',
 3),
('b3400001-0000-4000-8000-000000000004', 'hoang-thanh-thang-long', 'ST04', 'MEMORY_PAIRS',
 'Ghép cặp: hiện vật bảo tàng',
 '{"pairs":[{"id":"a","label":"Ngói men xanh"},{"id":"b","label":"Chim phượng"},{"id":"c","label":"Bát sứ ngự"},{"id":"d","label":"Chữ Quan"},{"id":"e","label":"Gạch hoa"},{"id":"f","label":"Lò quan"}]}',
 4),
('b3400001-0000-4000-8000-000000000005', 'hoang-thanh-thang-long', 'ST05', 'SPOT_DIFF',
 'Tìm khác biệt: hai lớp ký ức',
 '{"leftTitle":"Di sản cổ","rightTitle":"Không gian hiện đại","leftItems":["Nền móng khảo cổ","Ngói men trưng bày","Hầm chỉ huy quân sự","Rồng đá"],"rightItems":["Nền móng khảo cổ","Ngói men trưng bày","Không gian diễn giải hiện đại","Rồng đá"],"diffIndices":[2]}',
 5),
('b3400001-0000-4000-8000-000000000006', 'hoang-thanh-thang-long', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: kết hành trình',
 '{"scenario":"Bạn đứng gần Kỳ đài, muốn chụp ảnh biểu tượng Cột cờ và kết thúc tour an toàn.","choices":[{"id":"n","label":"Hướng Kỳ đài / Cột cờ — điểm kết chính thức","correct":true},{"id":"e","label":"Vào khu đang đóng cửa khảo cổ","correct":false},{"id":"s","label":"Rời khu không check-out","correct":false},{"id":"w","label":"Leo tường di tích","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;

INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST01', 'guide', 'UNESCO?', 'Hoàng thành Thăng Long được UNESCO công nhận vì lý do gì?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST02', 'guide', '18 Hoàng Diệu', 'Khu khảo cổ 18 Hoàng Diệu cho thấy những tầng văn hóa nào?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST03', 'guide', 'Rồng đá', 'Đôi rồng đá Điện Kính Thiên có ý nghĩa gì?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST04', 'guide', 'Ngói men', 'Ngói men xanh thời Lý nói lên điều gì về cung đình?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST05', 'guide', 'Hầm chỉ huy', 'Hầm chỉ huy trong Hoàng thành gắn với giai đoạn lịch sử nào?', 1),
(gen_random_uuid(), 'hoang-thanh-thang-long', 'ST06', 'guide', 'Kỳ đài', 'Cột cờ Hà Nội được xây dựng vào thời nào?', 1);

-- ========== Đại Nội Huế ==========
INSERT INTO stations (id, site_code, code, name, sort_order, lat, lng, quest_step_key, is_active) VALUES
('a3400011-0000-4000-8000-000000000001', 'dai-noi-hue', 'ST01', 'Ngọ Môn', 1, 16.469617, 107.579412, 'hue:ST01', TRUE),
('a3400011-0000-4000-8000-000000000002', 'dai-noi-hue', 'ST02', 'Điện Thái Hòa', 2, 16.470200, 107.579800, 'hue:ST02', TRUE),
('a3400011-0000-4000-8000-000000000003', 'dai-noi-hue', 'ST03', 'Thế Miếu', 3, 16.468900, 107.578500, 'hue:ST03', TRUE),
('a3400011-0000-4000-8000-000000000004', 'dai-noi-hue', 'ST04', 'Cung Kiến Trung', 4, 16.470800, 107.580200, 'hue:ST04', TRUE),
('a3400011-0000-4000-8000-000000000005', 'dai-noi-hue', 'ST05', 'Hồ Tịnh Tâm', 5, 16.471500, 107.578900, 'hue:ST05', TRUE),
('a3400011-0000-4000-8000-000000000006', 'dai-noi-hue', 'ST06', 'Lối kết hành trình', 6, 16.469000, 107.578000, 'hue:ST06', TRUE)
ON CONFLICT (site_code, code) DO NOTHING;

INSERT INTO station_content_blocks (id, station_id, block_type, title, body, sort_order) VALUES
('a3400012-0000-4000-8000-000000000001', 'a3400011-0000-4000-8000-000000000001', 'TEXT', 'Giới thiệu',
 'Ngọ Môn là cửa chính phía Nam của Hoàng thành Huế, gắn với Lầu Ngũ Phụng và các nghi lễ lớn triều Nguyễn.', 1),
('a3400012-0000-4000-8000-000000000002', 'a3400011-0000-4000-8000-000000000002', 'TEXT', 'Giới thiệu',
 'Điện Thái Hòa và sân Đại Triều Nghi là nơi đặt Ngai vàng — trung tâm nghi lễ đăng quang và triều hội.', 1),
('a3400012-0000-4000-8000-000000000003', 'a3400011-0000-4000-8000-000000000003', 'TEXT', 'Giới thiệu',
 'Thế Miếu thờ các vua triều Nguyễn — không gian tâm linh và ký ức dòng họ hoàng tộc.', 1),
('a3400012-0000-4000-8000-000000000004', 'a3400011-0000-4000-8000-000000000004', 'TEXT', 'Giới thiệu',
 'Cung Kiến Trung và khu vực cung cấm phản ánh đời sống nội cung — tuân thủ hướng dẫn BQL khi tham quan.', 1),
('a3400012-0000-4000-8000-000000000005', 'a3400011-0000-4000-8000-000000000005', 'TEXT', 'Giới thiệu',
 'Hồ Tịnh Tâm / khu vườn cung đình mang không khí thư thái giữa lòng kinh thành.', 1),
('a3400012-0000-4000-8000-000000000006', 'a3400011-0000-4000-8000-000000000006', 'TEXT', 'Giới thiệu',
 'Điểm kết hành trình: tổng kết sáu trạm Đại Nội và chia sẻ Hồ sơ giao liên.', 1)
ON CONFLICT (id) DO NOTHING;

INSERT INTO story_chapters (id, site_code, chapter_number, station_code, title, synopsis, requires_premium, sort_order) VALUES
('a3400013-0000-4000-8000-000000000001', 'dai-noi-hue', 1, 'ST01',
 'Một ngày trong Đại Nội — Chương 1: Qua Ngọ Môn',
 'Bạn bước qua cửa chính kinh thành, dưới bóng Lầu Ngũ Phụng — nơi từng diễn ra nghi lễ lớn của triều Nguyễn.',
 FALSE, 1),
('a3400013-0000-4000-8000-000000000002', 'dai-noi-hue', 2, 'ST02',
 'Một ngày trong Đại Nội — Chương 2: Trước Ngai vàng',
 'Sân Đại Triều Nghi và Điện Thái Hòa hé lộ nghi thức quyền lực — từ đăng quang đến triều hội ngày rằm.',
 FALSE, 2),
('a3400013-0000-4000-8000-000000000003', 'dai-noi-hue', 3, 'ST03',
 'Một ngày trong Đại Nội — Chương 3: Thế Miếu',
 'Không gian thờ các vua Nguyễn — bạn hiểu chữ hiếu và cách triều đại tự kể câu chuyện của mình.',
 TRUE, 3),
('a3400013-0000-4000-8000-000000000004', 'dai-noi-hue', 4, 'ST04',
 'Một ngày trong Đại Nội — Chương 4: Trong cung',
 'Khu cung cấm / Kiến Trung: đời sống nội cung và quy tắc tham quan hiện đại.',
 TRUE, 4),
('a3400013-0000-4000-8000-000000000005', 'dai-noi-hue', 5, 'ST05',
 'Một ngày trong Đại Nội — Chương 5: Bên hồ Tịnh Tâm',
 'Khoảng lặng giữa kinh thành — vườn và hồ kể về thẩm mỹ sống của cung đình.',
 TRUE, 5),
('a3400013-0000-4000-8000-000000000006', 'dai-noi-hue', 6, 'ST06',
 'Một ngày trong Đại Nội — Chương 6: Lối ra',
 'Kết thúc hành trình Đại Nội, bạn mang theo câu chuyện kinh thành Huế và mã giới thiệu.',
 TRUE, 6)
ON CONFLICT (site_code, chapter_number) DO NOTHING;

INSERT INTO station_minigames (id, site_code, station_code, game_type, title, config_json, sort_order) VALUES
('b3400011-0000-4000-8000-000000000001', 'dai-noi-hue', 'ST01', 'QUIZ_TIMED',
 'Trắc nghiệm: Ngọ Môn',
 '{"timeLimitSec":90,"questions":[{"prompt":"Ngọ Môn nằm ở hướng nào của Hoàng thành Huế?","options":["Phía Nam","Phía Bắc","Phía Đông"],"correctIndex":0},{"prompt":"Phía trên Ngọ Môn là công trình nào?","options":["Lầu Ngũ Phụng","Chùa Thiên Mụ","Kỳ đài"],"correctIndex":0},{"prompt":"Ngọ Môn gắn với triều đại nào?","options":["Nguyễn","Lý","Trần"],"correctIndex":0}]}',
 1),
('b3400011-0000-4000-8000-000000000002', 'dai-noi-hue', 'ST02', 'TIMELINE_ORDER',
 'Sắp thứ tự: nghi lễ triều Nguyễn',
 '{"items":["Vua vào Ngọ Môn","Triều thần tập trung sân Đại Triều","Nghi thức tại Điện Thái Hòa","Ban bố chiếu chỉ"],"hint":"Theo trình tự buổi đại triều điển hình."}',
 2),
('b3400011-0000-4000-8000-000000000003', 'dai-noi-hue', 'ST03', 'MATCH_TERMS',
 'Nối thuật ngữ: Thế Miếu',
 '{"terms":["Thế Miếu","Triều Nguyễn","Ngai vàng","Trùng thiềm điệp ốc"],"definitions":["Nơi thờ các vua","Triều đại kinh đô Huế","Biểu tượng quyền lực tại Thái Hòa","Kiểu nhà nối nhà đặc trưng Huế"],"answerKey":[0,1,2,3]}',
 3),
('b3400011-0000-4000-8000-000000000004', 'dai-noi-hue', 'ST04', 'MEMORY_PAIRS',
 'Ghép cặp: cung đình Huế',
 '{"pairs":[{"id":"a","label":"Cung Kiến Trung"},{"id":"b","label":"Nội cung"},{"id":"c","label":"Tả Vu"},{"id":"d","label":"Hữu Vu"},{"id":"e","label":"Hướng dẫn viên"},{"id":"f","label":"Quy tắc BQL"}]}',
 4),
('b3400011-0000-4000-8000-000000000005', 'dai-noi-hue', 'ST05', 'SPOT_DIFF',
 'Tìm khác biệt: không gian vườn hồ',
 '{"leftTitle":"Buổi sáng","rightTitle":"Buổi chiều","leftItems":["Hồ nước tĩnh","Bóng cây dài","Đường đi bộ mở","Nhóm đông"],"rightItems":["Hồ nước tĩnh","Bóng cây ngắn hơn","Đường đi bộ mở","Ít người hơn"],"diffIndices":[1,3]}',
 5),
('b3400011-0000-4000-8000-000000000006', 'dai-noi-hue', 'ST06', 'COMPASS_CHOICE',
 'Chọn hướng: kết thúc tour',
 '{"scenario":"Bạn đã đủ sáu trạm Đại Nội và muốn ra cổng an toàn để mở Wrapped.","choices":[{"id":"s","label":"Theo lối kết / cổng phụ được BQL chỉ dẫn","correct":true},{"id":"n","label":"Leo tường thành","correct":false},{"id":"e","label":"Vào khu đang tu bổ cấm vào","correct":false},{"id":"w","label":"Ở lại sau giờ đóng cửa","correct":false}]}',
 6)
ON CONFLICT (site_code, station_code) DO NOTHING;

INSERT INTO station_chat_prompts (id, site_code, station_code, persona, chip_label, question_text, sort_order) VALUES
(gen_random_uuid(), 'dai-noi-hue', 'ST01', 'guide', 'Ngọ Môn', 'Ngọ Môn dùng cho nghi lễ gì dưới triều Nguyễn?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST02', 'guide', 'Thái Hòa', 'Điện Thái Hòa có vai trò gì trong kinh thành?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST03', 'guide', 'Thế Miếu', 'Thế Miếu thờ những ai?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST04', 'guide', 'Cung cấm', 'Khi tham quan khu cung cần lưu ý gì?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST05', 'guide', 'Tịnh Tâm', 'Hồ Tịnh Tâm mang ý nghĩa gì trong cảnh quan cung đình?', 1),
(gen_random_uuid(), 'dai-noi-hue', 'ST06', 'guide', 'Kết tour', 'Sau sáu trạm Đại Nội nên làm gì tiếp theo trong TimeLens?', 1);
