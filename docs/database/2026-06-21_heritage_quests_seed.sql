-- Heritage quests: one main quest per location (content from dataset / AI docs)
BEGIN;

-- Cu Chi — keep canonical quest id; onsite check-in completes after online steps
INSERT INTO quests (
  id, location_id, title, description, story, points_reward, required_order,
  step_discovery_keys, completion_trigger, steps_total, cover_image
)
VALUES (
  '33333333-3333-3333-3333-333333333333',
  '11111111-1111-1111-1111-111111111111',
  'Hành trình dưới lòng đất',
  'Khám phá Bếp Hoàng Cầm, Phòng họp và Time Portal 1948 — sau đó check-in tại khu di tích Củ Chi.',
  'Năm 1968, giữa lòng đất Củ Chi, những con đường bí mật đã nuôi dưỡng niềm tin của một dân tộc. Hoàn thành hành trình để mở câu chuyện bí mật tại địa đạo.',
  100, 1,
  'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223,era:1948',
  'checkin', 4,
  '/media/heritage/dia-dao-cu-chi/cover.jpeg'
)
ON CONFLICT (id) DO UPDATE SET
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  story = EXCLUDED.story,
  points_reward = EXCLUDED.points_reward,
  step_discovery_keys = EXCLUDED.step_discovery_keys,
  completion_trigger = EXCLUDED.completion_trigger,
  steps_total = EXCLUDED.steps_total,
  cover_image = EXCLUDED.cover_image;

INSERT INTO quests (id, location_id, title, description, story, points_reward, required_order, step_discovery_keys, completion_trigger, steps_total, cover_image)
VALUES
(
  '44444444-4444-4444-4444-444444444401',
  '22222222-2222-2222-2222-222222222201',
  'Bước chân ra thế giới',
  'Chiêm ngưỡng rồng trên mái → Time Portal bến sông → vali mây.',
  'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Nhìn rồng trên mái, so sánh ảnh xưa–nay, rồi khám phá vali mây — không cần đọc hồ sơ dài.',
  120, 1,
  'artifact:ben-nha-rong-rong-mai,era:2026,artifact:ben-nha-rong-vali-may',
  'discovery', 3,
  '/media/heritage/ben-nha-rong/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444402',
  '22222222-2222-2222-2222-222222222202',
  'Ngọn tháp bên sông Hương',
  'Tháp Phước Duyên → Time Portal sông Hương → bia đá trên lưng rùa.',
  'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chiêm ngưỡng tháp, xem Time Portal, rồi mở bia đá — trải nghiệm bằng mắt.',
  100, 1,
  'artifact:chua-thien-mu-thap-phuoc-duyen,era:2026,artifact:chua-thien-mu-bia-da',
  'discovery', 3,
  '/media/heritage/chua-thien-mu/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444403',
  '22222222-2222-2222-2222-222222222203',
  'Dấu ấn kinh đô đầu tiên',
  'Long Sàng đá → Time Portal kinh đô → đền Lê Đại Hành.',
  '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Khám phá Long Sàng, so sánh ảnh xưa–nay, rồi mở đền Lê Đại Hành.',
  120, 1,
  'artifact:co-do-hoa-lu-long-sang,era:2026,artifact:co-do-hoa-lu-den-le-dai-hanh',
  'discovery', 3,
  '/media/heritage/co-do-hoa-lu/cover.jpeg'
),
(
  '44444444-4444-4444-4444-444444444404',
  '22222222-2222-2222-2222-222222222204',
  'Dấu ấn Hoàng Thành',
  'Móng cột cổ → Time Portal Thăng Long → ngói men xanh.',
  'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp hiện vật và Time Portal như một game khảo cổ.',
  150, 1,
  'artifact:hoang-thanh-thang-long-cot-co,era:2026,artifact:hoang-thanh-thang-long-ngoi-phuong',
  'discovery', 3,
  '/media/heritage/hoang-thanh-thang-long/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444405',
  '22222222-2222-2222-2222-222222222205',
  'Bí ẩn Chùa Cầu',
  'Chùa Cầu → Time Portal phố cổ → nhà Tấn Ký.',
  'Hội An không chỉ đẹp trong ảnh — mỗi mái ngói là một chuyến hàng từ phương xa. Nhìn Chùa Cầu, so sánh phố cổ, rồi mở nhà Tấn Ký.',
  100, 1,
  'artifact:pho-co-hoi-an-chua-cau,era:2026,artifact:pho-co-hoi-an-nha-co-tan-ky',
  'discovery', 3,
  '/media/heritage/pho-co-hoi-an/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444406',
  '22222222-2222-2222-2222-222222222206',
  'Cổ thành đá Tây Đô',
  'Cổng Nam → Time Portal Tây Đô → gạch in chữ.',
  'Thành không vữa nhưng đứng vững 600 năm — khám phá cổng Nam, Time Portal, rồi gạch in chữ Tây Đô.',
  120, 1,
  'artifact:thanh-nha-ho-cong-nam,era:2026,artifact:thanh-nha-ho-gach-tay-do',
  'discovery', 3,
  '/media/heritage/thanh-nha-ho/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444407',
  '22222222-2222-2222-2222-222222222207',
  'Đường đến khoa bảng',
  'Khuê Văn Các → Time Portal Văn Miếu → Hồ Thiên Quang.',
  'Trở thành môn sinh Quốc Tử: chiêm ngưỡng Khuê Văn Các, xem Time Portal, rồi mở Hồ Thiên Quang.',
  100, 1,
  'artifact:van-mieu-quoc-tu-giam-khue-van-cac,era:2026,artifact:van-mieu-quoc-tu-giam-ho-thien-quang',
  'discovery', 3,
  '/media/heritage/van-mieu-quoc-tu-giam/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444408',
  '22222222-2222-2222-2222-222222222208',
  'Bí ẩn Đại Nội',
  'Ngọ Môn → Time Portal Đại Nội → Cửu Đỉnh.',
  'Cung đình Huế im lặng nhưng đầy quyền lực. Chiêm ngưỡng Ngọ Môn, so sánh kinh thành, rồi mở Cửu Đỉnh.',
  150, 1,
  'artifact:dai-noi-hue-ngo-mon,era:2026,artifact:dai-noi-hue-cuu-dinh',
  'discovery', 3,
  '/media/heritage/dai-noi-hue/cover.webp'
),
(
  '44444444-4444-4444-4444-444444444409',
  '22222222-2222-2222-2222-222222222209',
  'Về Đất Tổ',
  'Trống đồng → Time Portal Nghĩa Lĩnh → cột đá thế.',
  'Về cội nguồn không cần vé máy bay — chiêm ngưỡng trống đồng, xem Time Portal, rồi mở cột đá thế.',
  120, 1,
  'artifact:den-hung-vuong-trong-dong,era:2026,artifact:den-hung-vuong-cot-da-the',
  'discovery', 3,
  '/media/heritage/den-hung-vuong/cover.webp'
)
ON CONFLICT (id) DO UPDATE SET
  location_id = EXCLUDED.location_id,
  title = EXCLUDED.title,
  description = EXCLUDED.description,
  story = EXCLUDED.story,
  points_reward = EXCLUDED.points_reward,
  step_discovery_keys = EXCLUDED.step_discovery_keys,
  completion_trigger = EXCLUDED.completion_trigger,
  steps_total = EXCLUDED.steps_total,
  cover_image = EXCLUDED.cover_image;

COMMIT;
