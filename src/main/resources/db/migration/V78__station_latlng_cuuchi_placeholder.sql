-- V78: Placeholder GPS coordinates for Củ Chi pilot stations ST01–ST06.
--
-- ⚠️  PLACEHOLDER — cần đo thực địa và thay bằng tọa độ chính xác.
-- Các tọa độ này nằm trong khuôn viên khu di tích Địa đạo Củ Chi (Ben Duoc)
-- và được bố trí dọc tuyến tham quan ~200m.
-- Sau khi field test: cập nhật lại bằng migration V79 hoặc UPDATE thủ công qua admin.
--
-- Ref GPS trung tâm khu Ben Duoc: 11.1416°N, 106.4616°E

UPDATE stations SET lat = 11.14155, lng = 106.46135
WHERE site_code = 'cu-chi' AND code = 'ST01'; -- Phòng họp (khu đón tiếp)

UPDATE stations SET lat = 11.14148, lng = 106.46148
WHERE site_code = 'cu-chi' AND code = 'ST02'; -- Kho ngầm

UPDATE stations SET lat = 11.14140, lng = 106.46162
WHERE site_code = 'cu-chi' AND code = 'ST03'; -- Giếng hầm & thông gió

UPDATE stations SET lat = 11.14133, lng = 106.46175
WHERE site_code = 'cu-chi' AND code = 'ST04'; -- Quân y

UPDATE stations SET lat = 11.14126, lng = 106.46188
WHERE site_code = 'cu-chi' AND code = 'ST05'; -- Xưởng vũ khí

UPDATE stations SET lat = 11.14119, lng = 106.46200
WHERE site_code = 'cu-chi' AND code = 'ST06'; -- Hầm phòng thủ (điểm cuối)
