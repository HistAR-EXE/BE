-- Patch 5 scene-link hotspots that commonly point the wrong way (Epic A calibrate).
-- Values from field calibrate pass (?calibrate=1) + spread overlapping BTL markers.

-- Bãi xe → Đền: nudge toward temple approach
UPDATE hotspots
SET yaw = 0.55, pitch = -0.48, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222221'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222222';

-- Đền cổng → Khu trưng bày: leftward far marker
UPDATE hotspots
SET yaw = -2.45, pitch = -0.50, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222222'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222224';

-- Ngã ba trưng bày → Bộ Tư lệnh: separate from overlapping siblings
UPDATE hotspots
SET yaw = 1.95, pitch = -0.48, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222224'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222223';

-- Bộ Tư lệnh → Khu ủy
UPDATE hotspots
SET yaw = 0.35, pitch = -0.45, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-222222222225';

-- Bộ Tư lệnh → Trưng bày bom: spread from Khu ủy marker
UPDATE hotspots
SET yaw = 1.15, pitch = -0.45, marker_style = 'far'
WHERE panorama_id = '22222222-2222-2222-2222-222222222223'
  AND type = 'scene'
  AND content_ref = '22222222-2222-2222-2222-22222222222e';
