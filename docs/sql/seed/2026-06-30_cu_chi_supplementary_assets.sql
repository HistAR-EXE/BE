-- Cu Chi: ảnh hiện vật / sinh hoạt từ folder "địa đạo củ chi" + gallery
BEGIN;

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:vu-khi';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi-2.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:cua-bom';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/che-tao-vu-khi-3.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:min-gat';

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/ham-giai-phau.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key IN ('artifact:tram-xa', 'artifact:den-dau');

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/vot-chong.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key IN ('artifact:chong-tre', 'artifact:lao-tre', 'artifact:bay');

UPDATE artifacts SET image_url = '/media/cu-chi/artifacts/lo-chau-mai.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'hotspot:vent';

UPDATE artifacts SET image_url = '/media/cu-chi/gallery/an-uong-duoi-dat.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'hotspot:kitchen';

UPDATE artifacts SET image_url = '/media/cu-chi/gallery/khoai-mi-1.jpg'
WHERE location_id = '11111111-1111-1111-1111-111111111111' AND unlock_key = 'artifact:cuoc-song-duoi-long-dat';

INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES
(
  'a0000001-0000-4000-8000-000000000021',
  '11111111-1111-1111-1111-111111111111',
  'Cối xay thóc dưới lòng đất',
  '/media/cu-chi/artifacts/coi-xay-thoc.jpg',
  'Cối xay thóc — chế biến lương thực tại căn cứ ngầm.',
  'Cuộc sống dưới địa đạo thiếu ánh sáng và thực phẩm. Cối xay thóc giúp chế biến lương thực ngay trong hầm, góp phần duy trì sinh hoạt của hàng nghìn người sống và chiến đấu tại Củ Chi.',
  'artifact:coi-xay-thoc',
  'primary',
  21
),
(
  'a0000001-0000-4000-8000-000000000022',
  '11111111-1111-1111-1111-111111111111',
  'Khoai mì — lương thực chiến khu',
  '/media/cu-chi/gallery/khoai-mi-2.jpg',
  'Khoai mì luộc muối mè — món ăn quen thuộc của cư dân địa đạo.',
  'Khoai mì là lương thực chính trong thời chiến tại Củ Chi. Du khách ngày nay vẫn được thử món khoai mì luộc muối mè — hương vị gắn với ký ức thời kháng chiến.',
  'artifact:khoai-mi',
  'primary',
  22
),
(
  'a0000001-0000-4000-8000-000000000023',
  '11111111-1111-1111-1111-111111111111',
  'Ngụy trang hầm chiến đấu',
  '/media/cu-chi/gallery/nguy-trang-ham-1.jpg',
  'Hệ thống ngụy trang tinh vi — khó phát hiện từ trên mặt đất.',
  'Dưới lớp đất và lá che phủ là cả một hệ thống cơ quan phức tạp: hầm chỉ huy, thông hơi, lối thoát hiểm. Ngụy trang giúp địa đạo trở thành "thành phố ngầm" bất khả xâm phạm.',
  'artifact:nguy-trang',
  'primary',
  23
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('artifact:vu-khi', 'artifact:coi-xay-thoc', '11111111-1111-1111-1111-111111111111'),
('hotspot:kitchen', 'artifact:khoai-mi', '11111111-1111-1111-1111-111111111111'),
('hotspot:vent', 'artifact:nguy-trang', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

COMMIT;
