-- Củ Chi: ảnh panorama 360° equirectangular từ Google Street View (1024×512, tỷ lệ 2:1)
-- FE static: /media/cu-chi/panoramas/*.png — Photo Sphere Viewer kéo xoay như Google Maps

-- Location Củ Chi: 11111111-1111-1111-1111-111111111111

-- 1) Cập nhật 3 scene hiện có (giữ UUID cho quest discovery scene:221 / scene:223)
UPDATE panoramas
SET
    image_url = '/media/cu-chi/panoramas/duong-vao.jpg',
    title = 'Địa đạo Củ Chi — Đường vào khu di tích'
WHERE id = '22222222-2222-2222-2222-222222222222';

UPDATE panoramas
SET
    image_url = '/media/cu-chi/panoramas/trung-bay-vu-khi.jpg',
    title = 'Khu trưng bày vũ khí ngoài trời'
WHERE id = '22222222-2222-2222-2222-222222222221';

UPDATE panoramas
SET
    image_url = '/media/cu-chi/panoramas/xe-thiet-giap.jpg',
    title = 'Xe thiết giáp M113'
WHERE id = '22222222-2222-2222-2222-222222222223';

-- 2) Thêm 2 scene mới (tour 5 điểm)
INSERT INTO panoramas (id, location_id, image_url, title)
VALUES
    (
        '22222222-2222-2222-2222-222222222224',
        '11111111-1111-1111-1111-111111111111',
        '/media/cu-chi/panoramas/san-le-tuong-niem.jpg',
        'Sân lễ Đài tưởng niệm'
    ),
    (
        '22222222-2222-2222-2222-222222222225',
        '11111111-1111-1111-1111-111111111111',
        '/media/cu-chi/panoramas/den-tuong-niem.jpg',
        'Đền tưởng niệm Bến Dước'
    )
ON CONFLICT (id) DO UPDATE
SET image_url = EXCLUDED.image_url, title = EXCLUDED.title;

-- 3) Cập nhật nhãn hotspot scene-link (đồ thị tour 5 scene)
UPDATE hotspots
SET label = '→ Khu trưng bày vũ khí'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND content_ref = '22222222-2222-2222-2222-222222222221'
  AND type = 'scene';

UPDATE hotspots
SET label = '→ Xe thiết giáp'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND content_ref = '22222222-2222-2222-2222-222222222223'
  AND type = 'scene';

UPDATE hotspots
SET label = '← Đường vào'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND content_ref = '22222222-2222-2222-2222-222222222222'
  AND type = 'scene';

UPDATE hotspots
SET label = '← Khu trưng bày'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND content_ref = '22222222-2222-2222-2222-222222222221'
  AND type = 'scene';

-- 4) Link mới giữa các scene
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222222', -1.2, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222224', '→ Sân lễ tưởng niệm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
      AND content_ref = '22222222-2222-2222-2222-222222222224'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222224', 2.0, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222225', '→ Đền tưởng niệm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
      AND content_ref = '22222222-2222-2222-2222-222222222225'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222224', -2.2, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222222', '← Đường vào'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
      AND content_ref = '22222222-2222-2222-2222-222222222222'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222225', -0.8, 0.1, 'scene',
       '22222222-2222-2222-2222-222222222224', '← Sân lễ'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222225'
      AND content_ref = '22222222-2222-2222-2222-222222222224'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222223', 1.8, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222225', '→ Đền tưởng niệm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
      AND content_ref = '22222222-2222-2222-2222-222222222225'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222225', 2.5, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222223', '← Xe thiết giáp'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222225'
      AND content_ref = '22222222-2222-2222-2222-222222222223'
);
