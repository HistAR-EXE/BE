-- Ảnh bìa nhiệm vụ 333 "Hành trình dưới lòng đất" là tệp duy nhất của Củ Chi còn nằm trên R2
-- (/media/heritage/...), nên thẻ nhiệm vụ hiện ô xám khi R2 chưa sẵn. Đổi sang ảnh trong FE/public
-- cho đồng bộ với 7 nhiệm vụ còn lại (V85). Ảnh này không trùng ảnh bước nào của nhiệm vụ.
-- Chạy lại an toàn.

UPDATE quests
SET cover_image = '/images/cu-chi/loi-xuong-ham.jpg'
WHERE id = '33333333-3333-3333-3333-333333333333'::uuid;
