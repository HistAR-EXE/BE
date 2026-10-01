-- Cu Chi: artifact story column, 3 missing artifacts, discovery links, demo admin account
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE artifacts ADD COLUMN IF NOT EXISTS story TEXT;

-- STT 19: Xưởng chế tạo vũ khí ngầm
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000018',
  '11111111-1111-1111-1111-111111111111',
  'Xưởng chế tạo vũ khí ngầm',
  '/media/cu-chi/artifacts/trung-bay-vu-khi.png',
  'Cưa bom lép lấy TNT — lấy vũ khí địch đánh địch.',
  'Giữa lòng đất Củ Chi thiếu thốn trăm bề, các chiến sĩ công binh đã thực hiện những nhiệm vụ nghẹt thở: Cưa bom lép của Mỹ để lấy thuốc nổ (TNT). Mỹ trút xuống Củ Chi hàng triệu tấn bom đạn, trong đó có một tỷ lệ nhỏ bị tịt (bom lép). Các chiến sĩ phải dùng cưa tay, vừa cưa vừa dội nước cực kỳ chậm rãi để tránh phát ra tia lửa gây nổ.

Với phương châm ''lấy vũ khí địch đánh địch'', từ nguồn thuốc nổ này, xưởng vũ khí ngầm ở tầng 2 đã đúc thành công lựu đạn chày, mìn định hướng (gạt chân), và đặc biệt là Mìn gạt chống tăng của Dũng sĩ Tô Hoài Đức. Những vũ khí tự chế thô sơ này chính là khắc tinh bẻ gãy các chiến dịch càn quét bằng cơ giới quy mô lớn như Crimp (1966) hay Cedar Falls (1967) của quân đội Mỹ.',
  'artifact:vu-khi',
  'primary',
  18
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- STT 20: Trạm xá hầm phẫu thuật
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000019',
  '11111111-1111-1111-1111-111111111111',
  'Trạm xá hầm phẫu thuật',
  '/media/cu-chi/artifacts/den-dau.jpg',
  'Cấp cứu dưới lòng đất — đèn dầu, thiếu thuốc, không khí ẩm.',
  'Trạm xá dưới lòng đất là nơi ranh giới giữa sự sống và cái chết mong manh nhất. Nằm sâu ở tầng 2, trong không gian ẩm ướt và thiếu dưỡng khí, các bác sĩ phải thực hiện những ca phẫu thuật phức tạp dưới ánh sáng leo lét của đèn dầu hoặc đèn pin quay tay bằng cơ học.

Điều kiện y tế vô cùng khắc nghiệt: Thiếu thốn thuốc gây mê, thuốc kháng sinh và băng gạc vô trùng. Để giữ vệ sinh, căn hầm phẫu thuật được lót xung quanh bằng các tấm nilon hoặc vải dù chiếm được của địch. Nhiều ca cưa chân, gắp mảnh bom, các y bác sĩ phải động viên chiến sĩ bằng tinh thần và lòng quả cảm. Nơi đây không chỉ cứu chữa hàng ngàn thương bệnh binh mà còn là nơi chứng kiến nhiều em bé Củ Chi cất tiếng khóc chào đời ngay trong lòng đất thép.',
  'artifact:tram-xa',
  'primary',
  19
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- STT 21: Khu trưng bày bẫy rập
INSERT INTO artifacts (id, location_id, name, image_url, description, story, unlock_key, reliability, sort_order)
VALUES (
  'a0000001-0000-4000-8000-000000000020',
  '11111111-1111-1111-1111-111111111111',
  'Khu trưng bày bẫy rập',
  '/media/cu-chi/artifacts/chong-tre.jpg',
  'Chông nắp, hom, cần — đòn đánh tâm lý nặng nề đối với quân viễn chinh.',
  'Trận đồ bẫy du kích tại Củ Chi là nỗi khiếp sợ hoảng loạn và là đòn đánh tâm lý nặng nề nhất đối với quân viễn chinh Mỹ. Lợi dụng địa hình rừng bụi rậm rạp, quân dân ta đã chế tạo ra hàng chục loại bẫy từ các nguyên liệu thô sơ như tre già ngâm nước (cho cứng và độc), sắt phế liệu, đinh đỉa.

Các loại bẫy tiêu biểu gồm: Chông nắp (địch giẫm lên nắp sẽ lật úp, rơi xuống hố chông), Chông hom (đâm ngược khi rút chân lên), Chông cần (khi vướng dây, cần tre kéo cọc chông đâm ngang hông). Bẫy không chỉ bố trí trên mặt đất mà còn cài cắm ngay tại các góc cua hẹp của địa đạo dưới hầm (hầm chữ Z) để chống lại lính ''chuột cống''. Địch một khi lọt vào trận đồ bẫy sẽ buộc phải dừng toàn bộ cuộc càn quét để cáng tải thương binh, làm tiêu hao sinh lực và kéo sụp ý chí tiến công.',
  'artifact:bay',
  'primary',
  20
)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name,
    image_url = EXCLUDED.image_url,
    description = EXCLUDED.description,
    story = EXCLUDED.story,
    reliability = EXCLUDED.reliability,
    sort_order = EXCLUDED.sort_order;

-- Discovery → artifact bridges for new POIs
INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('artifact:vu-khi', 'artifact:vu-khi', '11111111-1111-1111-1111-111111111111'),
('artifact:vu-khi', 'artifact:min-gat', '11111111-1111-1111-1111-111111111111'),
('artifact:vu-khi', 'artifact:cua-bom', '11111111-1111-1111-1111-111111111111'),
('artifact:tram-xa', 'artifact:tram-xa', '11111111-1111-1111-1111-111111111111'),
('artifact:tram-xa', 'artifact:den-dau', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:bay', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:chong-tre', '11111111-1111-1111-1111-111111111111'),
('artifact:bay', 'artifact:lao-tre', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

-- Story for Bếp Hoàng Cầm (existing)
UPDATE artifacts SET story = 'Chiến sĩ nuôi quân Hoàng Cầm sáng chế loại bếp dưới lòng đất từ năm 1951. Tại Địa đạo Củ Chi, bếp được đào vào sườn đất tầng hai — nơi chiến sĩ nấu ăn nuôi hàng nghìn người sống trong hầm.

Từ lò bếp, các đường rãnh thoát khói dài tỏa ra xa như "râu mực", phủ đất ẩm để lọc khói. Phần khói lên mặt đất chỉ còn làn hơi loãng như sương sớm — máy bay trinh sát Mỹ khó phát hiện. Bếp Hoàng Cầm góp phần thực hiện phương châm "nấu không khói" — một trong ba trụ cột sinh tồn: Đi không dấu, nấu không khói, nói không tiếng.'
WHERE unlock_key = 'hotspot:kitchen' AND location_id = '11111111-1111-1111-1111-111111111111';

-- Demo admin: xem toàn bộ cổ vật & discovery (role ADMIN + full seed unlock)
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
VALUES (
    'demo@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'ADMIN',
    'Demo Admin — Xem tất cả',
    NULL,
    10,
    999,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET
    role = 'ADMIN',
    display_name = EXCLUDED.display_name,
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    level = GREATEST(profiles.level, 10),
    total_points = GREATEST(profiles.total_points, 999);

-- Legacy platform admin (same password)
UPDATE profiles
SET password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'ADMIN',
    display_name = 'Platform Admin'
WHERE email = 'admin@histar.vn';

INSERT INTO user_artifacts (user_id, artifact_id, unlocked_at)
SELECT p.id, a.id, now()
FROM profiles p
CROSS JOIN artifacts a
WHERE p.email IN ('demo@histar.vn', 'admin@histar.vn')
  AND a.location_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id, artifact_id) DO NOTHING;

INSERT INTO user_discoveries (user_id, location_id, discovery_key, discovered_at)
SELECT p.id, dp.location_id, dp.unlock_key, now()
FROM profiles p
CROSS JOIN discovery_points dp
WHERE p.email IN ('demo@histar.vn', 'admin@histar.vn')
  AND dp.location_id = '11111111-1111-1111-1111-111111111111'
ON CONFLICT (user_id, location_id, discovery_key) DO NOTHING;

COMMIT;
