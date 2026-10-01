-- Quest mission keys: briefing → dialogue → reveal (decoupled from artifact pokédex)
BEGIN;

UPDATE quests SET
  step_discovery_keys = 'quest:444444444401:brief,quest:444444444401:dialogue,quest:444444444401:reveal',
  description = 'Ba chương: đọc hồ sơ Bến Nhà Rồng → đối thoại với hướng dẫn → giải mã manh mối tàu Amiral.',
  story = 'Ngày 5/6/1911, một bước chân tại bến sông đã mở ra hành trình 30 năm. Bạn là nhà khảo cổ thời gian — hãy khôi phục câu chuyện trước khi manh mối biến mất.'
WHERE id = '44444444-4444-4444-4444-444444444401';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444402:brief,quest:444444444402:dialogue,quest:444444444402:reveal',
  description = 'Ba chương bên sông Hương: truyền thuyết áo đỏ → đối thoại → giải mã tháp và bia.',
  story = 'Tiếng chuông chùa Thiên Mụ vang vọng qua bao thế kỷ. Chỉ người kiên nhẫn đọc, hỏi và giải mã mới nghe hết câu chuyện.'
WHERE id = '44444444-4444-4444-4444-444444444402';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444403:brief,quest:444444444403:dialogue,quest:444444444403:reveal',
  description = 'Ba chương kinh đô Hoa Lư: địa thế → trận Bạch Đằng → bảo vật đá.',
  story = '42 năm làm kinh đô, Hoa Lư giữ non sông thống nhất. Nhiệm vụ của bạn: chứng minh mình xứng đáng làm sứ giả kinh đô.'
WHERE id = '44444444-4444-4444-4444-444444444403';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444404:brief,quest:444444444404:dialogue,quest:444444444404:reveal',
  description = 'Ba chương Thăng Long: tầng lịch sử → đối thoại → khai quật số.',
  story = 'Dưới lòng phố Hà Nội hiện đại vẫn thở 13 thế kỷ quyền lực. Mở từng lớp như một game khảo cổ.'
WHERE id = '44444444-4444-4444-4444-444444444404';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444405:brief,quest:444444444405:dialogue,quest:444444444405:reveal',
  description = 'Ba chương phố cổ: cảng quốc tế → Chùa Cầu → khảm sành Phúc Kiến.',
  story = 'Hội An không chỉ đẹp trong ảnh — mỗi mái ngói là một chuyến hàng từ phương xa.'
WHERE id = '44444444-4444-4444-4444-444444444405';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444406:brief,quest:444444444406:dialogue,quest:444444444406:reveal',
  description = 'Ba chương Tây Đô: thành đá → chiến lược → gạch in chữ.',
  story = 'Thành không vữa nhưng đứng vững 600 năm — bạn có giải được bí mật kỹ thuật?'
WHERE id = '44444444-4444-4444-4444-444444444406';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444407:brief,quest:444444444407:dialogue,quest:444444444407:reveal',
  description = 'Ba chương Văn Miếu: khoa bảng → Khuê Văn Các → 82 bia Tiến sĩ.',
  story = 'Trở thành môn sinh Quốc Tử: đọc, hỏi, rồi khắc tên mình vào dòng khoa bảng — bằng hành động.'
WHERE id = '44444444-4444-4444-4444-444444444407';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444408:brief,quest:444444444408:dialogue,quest:444444444408:reveal',
  description = 'Ba chương Đại Nội: Ngọ Môn → điện Thái Hòa → Cửu Đỉnh.',
  story = 'Cung đình Huế im lặng nhưng đầy quyền lực. Chỉ sứ giả mới đi hết trục thiên tử.'
WHERE id = '44444444-4444-4444-4444-444444444408';

UPDATE quests SET
  step_discovery_keys = 'quest:444444444409:brief,quest:444444444409:dialogue,quest:444444444409:reveal',
  description = 'Ba chương Đền Hùng: giỗ Tổ → Lạc Hồng → trống đồng.',
  story = 'Về cội nguồn không cần vé máy bay — cần lòng tò mò và ba chương kiên trì.'
WHERE id = '44444444-4444-4444-4444-444444444409';

COMMIT;
