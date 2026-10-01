-- Add unlock_key to photo_scenes for Time Portal discovery alignment
BEGIN;

ALTER TABLE photo_scenes ADD COLUMN IF NOT EXISTS unlock_key VARCHAR(128);

UPDATE photo_scenes SET unlock_key = 'photo:cua-ham'
WHERE id = '50000001-0000-4000-8000-000000000001';

UPDATE photo_scenes SET unlock_key = 'scene:22222222-2222-2222-2222-222222222221'
WHERE id = '50000001-0000-4000-8000-000000000002';

UPDATE photo_scenes SET unlock_key = 'scene:22222222-2222-2222-2222-222222222223'
WHERE id = '50000001-0000-4000-8000-000000000003';

UPDATE photo_scenes SET unlock_key = 'hotspot:vent'
WHERE id = '50000001-0000-4000-8000-000000000004';

UPDATE photo_scenes SET unlock_key = 'photo:gieng'
WHERE id = '50000001-0000-4000-8000-000000000005';

COMMIT;
