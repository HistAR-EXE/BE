-- CP3 Week 1: citation sources for AI chat system prompt (internal, not exposed via API)
ALTER TABLE locations ADD COLUMN IF NOT EXISTS sources TEXT;

UPDATE locations
SET sources = 'Khu di tích lịch sử Địa đạo Củ Chi; Ban Quản lý Di tích Củ Chi; Tài liệu UNESCO về Di sản Thế giới'
WHERE id = '11111111-1111-1111-1111-111111111111';

UPDATE locations
SET sources = 'Khu Di tích Cố đô Huế; Ban Quản lý Di tích Cố đô Huế'
WHERE name ILIKE '%Huế%' OR name ILIKE '%Hue%';

UPDATE locations
SET sources = 'Khu Di tích Hoàng thành Thăng Long; Ban Quản lý Di tích Hoàng thành Thăng Long'
WHERE name ILIKE '%Thăng Long%' OR name ILIKE '%Hà Nội%';

UPDATE locations
SET sources = 'Khu di tích lịch sử ' || name
WHERE sources IS NULL;
