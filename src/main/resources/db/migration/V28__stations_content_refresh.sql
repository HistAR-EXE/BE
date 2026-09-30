-- Week 0: align Củ Chi pilot station names with MVBP doc (ST01–ST06 codes unchanged).
UPDATE stations SET name = 'Phòng họp', sort_order = 1
WHERE site_code = 'cu-chi' AND code = 'ST01';

UPDATE stations SET name = 'Kho ngầm', sort_order = 2
WHERE site_code = 'cu-chi' AND code = 'ST02';

UPDATE stations SET name = 'Giếng hầm & thông gió', sort_order = 3
WHERE site_code = 'cu-chi' AND code = 'ST03';

UPDATE stations SET name = 'Quân y', sort_order = 4
WHERE site_code = 'cu-chi' AND code = 'ST04';

UPDATE stations SET name = 'Xưởng vũ khí', sort_order = 5
WHERE site_code = 'cu-chi' AND code = 'ST05';

UPDATE stations SET name = 'Hầm phòng thủ', sort_order = 6
WHERE site_code = 'cu-chi' AND code = 'ST06';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Phòng họp',
    body = 'Trạm mở đầu hành trình: không gian chỉ huy và họp bí mật dưới lòng đất. Nội dung chi tiết sẽ được BQL xác thực sau khảo sát Tuần 0.'
WHERE id = 'a3000002-0000-4000-8000-000000000001';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Kho ngầm',
    body = 'Kho lương thực và khu bếp (nếu nằm trên tuyến tham quan). Chủ đề: nuôi quân trong im lặng, giấu khói và bảo quản lương khô.'
WHERE id = 'a3000002-0000-4000-8000-000000000002';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Giếng hầm & thông gió',
    body = 'Giếng nước, lỗ thông hơi và cách ngụy trang miệng giếng. Chủ đề: nước sạch và không khí cho sinh hoạt dưới hầm.'
WHERE id = 'a3000002-0000-4000-8000-000000000003';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Quân y',
    body = 'Khu chăm sóc người bị thương với điều kiện y tế thiếu thốn. Chủ đề: thuốc thảo dược, sơ cứu dưới ánh đèn dầu.'
WHERE id = 'a3000002-0000-4000-8000-000000000004';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Xưởng vũ khí',
    body = 'Xưởng tái chế vật liệu thành vũ khí và công cụ. Chủ đề: an toàn lao động, quy mô xưởng ngầm.'
WHERE id = 'a3000002-0000-4000-8000-000000000005';

UPDATE station_content_blocks SET
    title = 'Giới thiệu Hầm phòng thủ',
    body = 'Hệ thống hầm phòng thủ, cửa thoát và lỗ quan sát. Chủ đề: trực gác và sinh hoạt khi bị vây.'
WHERE id = 'a3000002-0000-4000-8000-000000000006';
