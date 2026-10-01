-- P1 mixed visual pipeline: artifact → era:2026 portal → artifact (9 heritage quests)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'artifact:ben-nha-rong-rong-mai,era:2026,artifact:ben-nha-rong-vali-may',
  description = 'Chiêm ngưỡng rồng trên mái → Time Portal bến sông → vali mây.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'artifact:chua-thien-mu-thap-phuoc-duyen,era:2026,artifact:chua-thien-mu-bia-da',
  description = 'Tháp Phước Duyên → Time Portal sông Hương → bia đá trên lưng rùa.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'artifact:co-do-hoa-lu-long-sang,era:2026,artifact:co-do-hoa-lu-den-le-dai-hanh',
  description = 'Long Sàng đá → Time Portal kinh đô → đền Lê Đại Hành.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'artifact:hoang-thanh-thang-long-cot-co,era:2026,artifact:hoang-thanh-thang-long-ngoi-phuong',
  description = 'Móng cột cổ → Time Portal Thăng Long → ngói men xanh.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'artifact:pho-co-hoi-an-chua-cau,era:2026,artifact:pho-co-hoi-an-nha-co-tan-ky',
  description = 'Chùa Cầu → Time Portal phố cổ → nhà Tấn Ký.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'artifact:thanh-nha-ho-cong-nam,era:2026,artifact:thanh-nha-ho-gach-tay-do',
  description = 'Cổng Nam → Time Portal Tây Đô → gạch in chữ.'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'artifact:van-mieu-quoc-tu-giam-khue-van-cac,era:2026,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  description = 'Khuê Văn Các → Time Portal Văn Miếu → Hồ Thiên Quang.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'artifact:dai-noi-hue-ngo-mon,era:2026,artifact:dai-noi-hue-cuu-dinh',
  description = 'Ngọ Môn → Time Portal Đại Nội → Cửu Đỉnh.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'artifact:den-hung-vuong-trong-dong,era:2026,artifact:den-hung-vuong-cot-da-the',
  description = 'Trống đồng → Time Portal Nghĩa Lĩnh → cột đá thế.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;
