-- Quest visual flow: artifact discovery keys (see objects, not read text)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'artifact:ben-nha-rong-rong-mai,artifact:ben-nha-rong-tau-amiral,artifact:ben-nha-rong-vali-may',
  description = 'Chiêm ngưỡng 3 hiện vật Bến Nhà Rồng — rồng trên mái, tàu Amiral, vali mây.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'artifact:chua-thien-mu-thap-phuoc-duyen,artifact:chua-thien-mu-dai-hong-chung,artifact:chua-thien-mu-bia-da',
  description = 'Xem 3 hiện vật Chùa Thiên Mụ — tháp, chuông và bia đá.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'artifact:co-do-hoa-lu-long-sang,artifact:co-do-hoa-lu-cot-kinh,artifact:co-do-hoa-lu-den-le-dai-hanh',
  description = 'Khám phá Long Sàng, cột kinh và đền Lê Đại Hành qua hiện vật số.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'artifact:hoang-thanh-thang-long-cot-co,artifact:hoang-thanh-thang-long-doi-rong-da,artifact:hoang-thanh-thang-long-ngoi-phuong',
  description = 'Khai quật số — móng cột, rồng đá và ngói men xanh Hoàng Thành.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'artifact:pho-co-hoi-an-chua-cau,artifact:pho-co-hoi-an-hoi-quan-phuc-kien,artifact:pho-co-hoi-an-nha-co-tan-ky',
  description = 'Nhìn Chùa Cầu, hội quán Phúc Kiến và nhà Tấn Ký qua tủ hiện vật.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'artifact:thanh-nha-ho-cong-nam,artifact:thanh-nha-ho-khoi-da,artifact:thanh-nha-ho-gach-tay-do',
  description = 'Thành đá Tây Đô — cổng Nam, khối đá và gạch in chữ.'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'artifact:van-mieu-quoc-tu-giam-khue-van-cac,artifact:van-mieu-quoc-tu-giam-bia-tien-si,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  description = 'Khuê Văn Các, 82 bia Tiến sĩ và Hồ Thiên Quang — trải nghiệm bằng mắt.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'artifact:dai-noi-hue-ngo-mon,artifact:dai-noi-hue-ngai-vang,artifact:dai-noi-hue-cuu-dinh',
  description = 'Ngọ Môn, ngai vàng và Cửu Đỉnh — bảo vật triều Nguyễn trong tủ sưu tập.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'artifact:den-hung-vuong-trong-dong,artifact:den-hung-vuong-banh-chung,artifact:den-hung-vuong-cot-da-the',
  description = 'Trống đồng, bánh chưng và cột đá — cội nguồn Lạc Hồng qua hiện vật.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;
