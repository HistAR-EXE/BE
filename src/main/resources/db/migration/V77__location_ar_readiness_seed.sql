-- V77: Seed is_ar_available for all 10 heritage locations.
-- Column was added via Hibernate DDL; this migration sets the authoritative values.
-- Admin can toggle individual sites via PATCH /api/admin/locations/{id} (W1.2).
--
-- READY (has panoramas + stations + quest content):
--   Củ Chi (11111111-...) — full content, panoramas, minigames
--   Hoàng Thành Thăng Long (204) — content seeded in narrative pilots
--
-- NOT READY (content pending — see Wave 3 of GO_TO_MARKET_PLAN):
--   All others — enable via admin console when content is complete.

UPDATE locations
SET is_ar_available = TRUE
WHERE id IN (
    '11111111-1111-1111-1111-111111111111',  -- Địa Đạo Củ Chi
    '22222222-2222-2222-2222-222222222204'   -- Hoàng Thành Thăng Long
);

UPDATE locations
SET is_ar_available = FALSE
WHERE id IN (
    '22222222-2222-2222-2222-222222222209',  -- Đền Hùng Vương
    '22222222-2222-2222-2222-222222222207',  -- Văn Miếu Quốc Tử Giám
    '22222222-2222-2222-2222-222222222203',  -- Cố Đô Hoa Lư
    '22222222-2222-2222-2222-222222222206',  -- Thành Nhà Hồ
    '22222222-2222-2222-2222-222222222202',  -- Chùa Thiên Mụ
    '22222222-2222-2222-2222-222222222208',  -- Đại Nội Huế
    '22222222-2222-2222-2222-222222222205'   -- Phố Cổ Hội An
)
  AND (is_ar_available IS NULL OR is_ar_available = FALSE);
-- Note: 10th location (Dinh Độc Lập / Ben Nha Rong) — not seeded in V8, check before enabling.
