-- P1 support data: discovery_points + photo_pairs for 9 heritage quest pipelines
BEGIN;

-- Quest step keys must exist in discovery_points (DiscoveryServiceImpl + SeedConsistencyValidator)
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order) VALUES
-- Bến Nhà Rồng
('d0000002-0000-4000-8000-000000000201', '22222222-2222-2222-2222-222222222201', 'Hai con rồng trên mái', 42.00, 38.00, 'artifact:ben-nha-rong-rong-mai', 1),
('d0000002-0000-4000-8000-000000000202', '22222222-2222-2222-2222-222222222201', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000203', '22222222-2222-2222-2222-222222222201', 'Chiếc vali mây', 58.00, 42.00, 'artifact:ben-nha-rong-vali-may', 3),
-- Chùa Thiên Mụ
('d0000002-0000-4000-8000-000000000211', '22222222-2222-2222-2222-222222222202', 'Tháp Phước Duyên', 42.00, 38.00, 'artifact:chua-thien-mu-thap-phuoc-duyen', 1),
('d0000002-0000-4000-8000-000000000212', '22222222-2222-2222-2222-222222222202', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000213', '22222222-2222-2222-2222-222222222202', 'Bia đá trên lưng rùa', 58.00, 42.00, 'artifact:chua-thien-mu-bia-da', 3),
-- Cố đô Hoa Lư
('d0000002-0000-4000-8000-000000000221', '22222222-2222-2222-2222-222222222203', 'Long Sàng đá', 42.00, 38.00, 'artifact:co-do-hoa-lu-long-sang', 1),
('d0000002-0000-4000-8000-000000000222', '22222222-2222-2222-2222-222222222203', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000223', '22222222-2222-2222-2222-222222222203', 'Đền Lê Đại Hành', 58.00, 42.00, 'artifact:co-do-hoa-lu-den-le-dai-hanh', 3),
-- Hoàng Thành Thăng Long
('d0000002-0000-4000-8000-000000000231', '22222222-2222-2222-2222-222222222204', 'Móng cột cổ', 42.00, 38.00, 'artifact:hoang-thanh-thang-long-cot-co', 1),
('d0000002-0000-4000-8000-000000000232', '22222222-2222-2222-2222-222222222204', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000233', '22222222-2222-2222-2222-222222222204', 'Ngói men xanh', 58.00, 42.00, 'artifact:hoang-thanh-thang-long-ngoi-phuong', 3),
-- Phố cổ Hội An
('d0000002-0000-4000-8000-000000000241', '22222222-2222-2222-2222-222222222205', 'Chùa Cầu', 42.00, 38.00, 'artifact:pho-co-hoi-an-chua-cau', 1),
('d0000002-0000-4000-8000-000000000242', '22222222-2222-2222-2222-222222222205', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000243', '22222222-2222-2222-2222-222222222205', 'Nhà cổ Tấn Ký', 58.00, 42.00, 'artifact:pho-co-hoi-an-nha-co-tan-ky', 3),
-- Thành Nhà Hồ
('d0000002-0000-4000-8000-000000000251', '22222222-2222-2222-2222-222222222206', 'Cổng Nam', 42.00, 38.00, 'artifact:thanh-nha-ho-cong-nam', 1),
('d0000002-0000-4000-8000-000000000252', '22222222-2222-2222-2222-222222222206', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000253', '22222222-2222-2222-2222-222222222206', 'Gạch in chữ Tây Đô', 58.00, 42.00, 'artifact:thanh-nha-ho-gach-tay-do', 3),
-- Văn Miếu – Quốc Tử Giám
('d0000002-0000-4000-8000-000000000261', '22222222-2222-2222-2222-222222222207', 'Khuê Văn Các', 42.00, 38.00, 'artifact:van-mieu-quoc-tu-giam-khue-van-cac', 1),
('d0000002-0000-4000-8000-000000000262', '22222222-2222-2222-2222-222222222207', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000263', '22222222-2222-2222-2222-222222222207', 'Hồ Thiên Quang', 58.00, 42.00, 'artifact:van-mieu-quoc-tu-giam-ho-thien-quang', 3),
-- Đại Nội Huế
('d0000002-0000-4000-8000-000000000271', '22222222-2222-2222-2222-222222222208', 'Ngọ Môn', 42.00, 38.00, 'artifact:dai-noi-hue-ngo-mon', 1),
('d0000002-0000-4000-8000-000000000272', '22222222-2222-2222-2222-222222222208', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000273', '22222222-2222-2222-2222-222222222208', 'Cửu Đỉnh', 58.00, 42.00, 'artifact:dai-noi-hue-cuu-dinh', 3),
-- Đền Hùng
('d0000002-0000-4000-8000-000000000281', '22222222-2222-2222-2222-222222222209', 'Trống đồng Đông Sơn', 42.00, 38.00, 'artifact:den-hung-vuong-trong-dong', 1),
('d0000002-0000-4000-8000-000000000282', '22222222-2222-2222-2222-222222222209', 'Time Portal 2026', 50.00, 50.00, 'era:2026', 2),
('d0000002-0000-4000-8000-000000000283', '22222222-2222-2222-2222-222222222209', 'Cột đá thế', 58.00, 42.00, 'artifact:den-hung-vuong-cot-da-the', 3)
ON CONFLICT (location_id, unlock_key) DO UPDATE
SET name = EXCLUDED.name, map_x_pct = EXCLUDED.map_x_pct, map_y_pct = EXCLUDED.map_y_pct, sort_order = EXCLUDED.sort_order;

-- Time Portal fallback: one photo pair per heritage site (Then/Now slider)
DELETE FROM photo_pairs
WHERE location_id IN (
  '22222222-2222-2222-2222-222222222201',
  '22222222-2222-2222-2222-222222222202',
  '22222222-2222-2222-2222-222222222203',
  '22222222-2222-2222-2222-222222222204',
  '22222222-2222-2222-2222-222222222205',
  '22222222-2222-2222-2222-222222222206',
  '22222222-2222-2222-2222-222222222207',
  '22222222-2222-2222-2222-222222222208',
  '22222222-2222-2222-2222-222222222209'
);

INSERT INTO photo_pairs (location_id, historical_image, current_image, year, caption, sort_order) VALUES
('22222222-2222-2222-2222-222222222201', '/media/heritage/ben-nha-rong/cover.webp', '/media/heritage/ben-nha-rong/cover.webp', 1911, 'Bến Nhà Rồng — xưa và nay', 1),
('22222222-2222-2222-2222-222222222202', '/media/heritage/chua-thien-mu/cover.webp', '/media/heritage/chua-thien-mu/cover.webp', 1844, 'Chùa Thiên Mụ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222203', '/media/heritage/co-do-hoa-lu/cover.jpeg', '/media/heritage/co-do-hoa-lu/cover.jpeg', 968, 'Cố đô Hoa Lư — xưa và nay', 1),
('22222222-2222-2222-2222-222222222204', '/media/heritage/hoang-thanh-thang-long/cover.webp', '/media/heritage/hoang-thanh-thang-long/cover.webp', 1010, 'Hoàng Thành Thăng Long — xưa và nay', 1),
('22222222-2222-2222-2222-222222222205', '/media/heritage/pho-co-hoi-an/cover.webp', '/media/heritage/pho-co-hoi-an/cover.webp', 1600, 'Phố cổ Hội An — xưa và nay', 1),
('22222222-2222-2222-2222-222222222206', '/media/heritage/thanh-nha-ho/cover.webp', '/media/heritage/thanh-nha-ho/cover.webp', 1397, 'Thành Nhà Hồ — xưa và nay', 1),
('22222222-2222-2222-2222-222222222207', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp', '/media/heritage/van-mieu-quoc-tu-giam/cover.webp', 1070, 'Văn Miếu — xưa và nay', 1),
('22222222-2222-2222-2222-222222222208', '/media/heritage/dai-noi-hue/cover.webp', '/media/heritage/dai-noi-hue/cover.webp', 1802, 'Đại Nội Huế — xưa và nay', 1),
('22222222-2222-2222-2222-222222222209', '/media/heritage/den-hung-vuong/cover.webp', '/media/heritage/den-hung-vuong/cover.webp', 2879, 'Đền Hùng — xưa và nay', 1);

COMMIT;
