-- CP3 Week 3: Virtual tour 3 scene Củ Chi + hotspot scene-link (UUID)
-- Local: dùng placehold (đủ demo). Production: thay image_url bằng MinIO HTTPS sau khi chụp thật.

-- Location Củ Chi: 11111111-1111-1111-1111-111111111111

-- 1) Panorama chính — cổng vào
UPDATE panoramas
SET
    image_url = COALESCE(NULLIF(image_url, ''), 'https://placehold.co/4096x2048?text=Cu+Chi+Cong+Vao'),
    title = 'Địa đạo Củ Chi — Cổng vào'
WHERE id = '22222222-2222-2222-2222-222222222222';

-- 2) Scene phụ (virtual tour đa scene)
INSERT INTO panoramas (id, location_id, image_url, title)
VALUES
    (
        '22222222-2222-2222-2222-222222222221',
        '11111111-1111-1111-1111-111111111111',
        'https://placehold.co/4096x2048?text=Bep+Hoang+Cam',
        'Bếp Hoàng Cầm'
    ),
    (
        '22222222-2222-2222-2222-222222222223',
        '11111111-1111-1111-1111-111111111111',
        'https://placehold.co/4096x2048?text=Phong+Hop',
        'Phòng họp dưới lòng đất'
    )
ON CONFLICT (id) DO UPDATE
SET image_url = EXCLUDED.image_url, title = EXCLUDED.title;

-- 2b) Sửa hotspot legacy: content_ref 'meeting-room' → UUID scene (FE Virtual Tour cần UUID)
UPDATE hotspots
SET type = 'scene', content_ref = '22222222-2222-2222-2222-222222222223'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND content_ref = 'meeting-room';

-- 3) Hotspot chuyển scene (type = scene, content_ref = panorama id đích)
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT
    '22222222-2222-2222-2222-222222222222',
    1.5, 0.0, 'scene', '22222222-2222-2222-2222-222222222221', '→ Bếp Hoàng Cầm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
      AND content_ref = '22222222-2222-2222-2222-222222222221'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT
    '22222222-2222-2222-2222-222222222221',
    -1.0, 0.1, 'scene', '22222222-2222-2222-2222-222222222223', '→ Phòng họp'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots
    WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
      AND content_ref = '22222222-2222-2222-2222-222222222223'
);

-- 3b) Link quay lại (optional nhưng tốt cho demo)
INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222221', -2.5, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222222', '← Cổng vào'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
      AND content_ref = '22222222-2222-2222-2222-222222222222'
);

INSERT INTO hotspots (panorama_id, yaw, pitch, type, content_ref, label)
SELECT '22222222-2222-2222-2222-222222222223', 0.5, 0.0, 'scene',
       '22222222-2222-2222-2222-222222222221', '← Bếp Hoàng Cầm'
WHERE NOT EXISTS (
    SELECT 1 FROM hotspots WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
      AND content_ref = '22222222-2222-2222-2222-222222222221'
);

-- 4) (Tùy chọn) Cập nhật photo-pairs xưa/nay nếu có ảnh thật từ chuyến đi
-- UPDATE photo_pairs SET historical_image = 'https://...', current_image = 'https://...'
-- WHERE location_id = '11111111-1111-1111-1111-111111111111';
