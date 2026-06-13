-- HistAR Cu Chi — verify unlock-key / era alignment (run on demo Postgres before pitch freeze)
-- Location: 11111111-1111-1111-1111-111111111111
-- Usage: psql -f verify-unlock-alignment.sql  (or paste in pgAdmin)

\echo '=== VERIFY A1: POI without direct artifact AND missing bridge link ==='
SELECT dp.unlock_key AS discovery_key, dp.name
FROM discovery_points dp
WHERE dp.location_id = '11111111-1111-1111-1111-111111111111'
  AND NOT EXISTS (
    SELECT 1 FROM artifacts a
    WHERE a.location_id = dp.location_id AND a.unlock_key = dp.unlock_key)
  AND NOT EXISTS (
    SELECT 1 FROM discovery_artifact_links l
    WHERE l.discovery_unlock_key = dp.unlock_key
      AND l.location_id = dp.location_id);

\echo '=== VERIFY A1b: Bep + Gieng bridge links (expect 2 rows) ==='
SELECT discovery_unlock_key, artifact_unlock_key, location_id
FROM discovery_artifact_links
WHERE discovery_unlock_key IN (
  'scene:22222222-2222-2222-2222-222222222221',
  'photo:gieng');

\echo '=== VERIFY A2: Era POI keys (expect era:1948, era:1968, era:2026) ==='
SELECT unlock_key, name
FROM discovery_points
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND unlock_key LIKE 'era:%'
ORDER BY unlock_key;

\echo '=== VERIFY A2b: time_layers eras (expect 1948, 1968, 2026) ==='
SELECT DISTINCT era FROM time_layers ORDER BY era;

-- =============================================================================
-- FIX (run only if verify shows gaps — safe to re-run with ON CONFLICT / WHERE)
-- =============================================================================

\echo '=== FIX A1: Insert missing discovery_artifact_links ==='
INSERT INTO discovery_artifact_links (discovery_unlock_key, artifact_unlock_key, location_id) VALUES
('scene:22222222-2222-2222-2222-222222222221', 'hotspot:kitchen', '11111111-1111-1111-1111-111111111111'),
('photo:gieng', 'scene:gieng', '11111111-1111-1111-1111-111111111111')
ON CONFLICT (discovery_unlock_key, artifact_unlock_key) DO NOTHING;

\echo '=== FIX A2: Align era:1967 POI to era:1948 (1:1 with Time Portal tabs) ==='
UPDATE discovery_points
SET unlock_key = 'era:1948', name = '1948 — khởi đầu hầm ngầm'
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND unlock_key = 'era:1967';

UPDATE quests
SET step_discovery_keys = REPLACE(step_discovery_keys, 'era:1967', 'era:1948')
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND step_discovery_keys LIKE '%era:1967%';

UPDATE discovery_content_bindings
SET unlock_key = 'era:1948',
    record_key = 'era:1948',
    href_template = '/time-portal/{locationId}?era=1948'
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND unlock_key = 'era:1967';

-- Migrate existing user_discoveries if DB had era:1967 before fix
UPDATE user_discoveries ud
SET unlock_key = 'era:1948'
WHERE unlock_key = 'era:1967'
  AND EXISTS (
    SELECT 1 FROM discovery_points dp
    WHERE dp.location_id = '11111111-1111-1111-1111-111111111111'
      AND dp.unlock_key = 'era:1948');

\echo '=== Re-run VERIFY queries after FIX ==='
