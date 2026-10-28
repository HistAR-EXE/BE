-- V104's candidate ID was already assigned to a legacy tour scene in some
-- databases. Create the 1975 key with a fresh UUID; keep every old point.
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
SELECT gen_random_uuid(), '11111111-1111-1111-1111-111111111111'::uuid,
       'Ngày giải phóng 1975', 50.00, 28.00, 'era:1975', 14
WHERE EXISTS (SELECT 1 FROM locations WHERE id = '11111111-1111-1111-1111-111111111111'::uuid)
  AND NOT EXISTS (
      SELECT 1 FROM discovery_points
      WHERE location_id = '11111111-1111-1111-1111-111111111111'::uuid
        AND unlock_key = 'era:1975'
  )
ON CONFLICT DO NOTHING;
