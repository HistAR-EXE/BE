-- Sửa tiếng Việt bị lỗi encoding (PowerShell pipe) + nhãn hotspot tour 360°

UPDATE panoramas SET title = 'Địa đạo Củ Chi — Đường vào khu di tích'
WHERE id = '22222222-2222-2222-2222-222222222222';

UPDATE panoramas SET title = 'Khu trưng bày vũ khí ngoài trời'
WHERE id = '22222222-2222-2222-2222-222222222221';

UPDATE panoramas SET title = 'Xe thiết giáp M113'
WHERE id = '22222222-2222-2222-2222-222222222223';

UPDATE panoramas SET title = 'Sân lễ Đài tưởng niệm'
WHERE id = '22222222-2222-2222-2222-222222222224';

UPDATE panoramas SET title = 'Đền tưởng niệm Bến Dước'
WHERE id = '22222222-2222-2222-2222-222222222225';

UPDATE hotspots SET label = '→ Khu trưng bày vũ khí'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND content_ref = '22222222-2222-2222-2222-222222222221' AND type = 'scene';

UPDATE hotspots SET label = '→ Sân lễ tưởng niệm'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND content_ref = '22222222-2222-2222-2222-222222222224' AND type = 'scene';

UPDATE hotspots SET label = '→ Xe thiết giáp'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND content_ref = '22222222-2222-2222-2222-222222222223' AND type = 'scene';

UPDATE hotspots SET label = '← Đường vào'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND content_ref = '22222222-2222-2222-2222-222222222222' AND type = 'scene';

UPDATE hotspots SET label = '← Khu trưng bày'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND content_ref = '22222222-2222-2222-2222-222222222221' AND type = 'scene';

UPDATE hotspots SET label = '→ Đền tưởng niệm'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND content_ref = '22222222-2222-2222-2222-222222222225' AND type = 'scene';

UPDATE hotspots SET label = '→ Đền tưởng niệm'
WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
  AND content_ref = '22222222-2222-2222-2222-222222222225' AND type = 'scene';

UPDATE hotspots SET label = '← Đường vào'
WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
  AND content_ref = '22222222-2222-2222-2222-222222222222' AND type = 'scene';

UPDATE hotspots SET label = '← Sân lễ'
WHERE panorama_id = '22222222-2222-2222-2222-222222222225'
  AND content_ref = '22222222-2222-2222-2222-222222222224' AND type = 'scene';

UPDATE hotspots SET label = '← Xe thiết giáp'
WHERE panorama_id = '22222222-2222-2222-2222-222222222225'
  AND content_ref = '22222222-2222-2222-2222-222222222223' AND type = 'scene';
