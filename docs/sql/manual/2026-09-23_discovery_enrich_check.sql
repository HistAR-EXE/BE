-- Read-only check. Table is discovery_content_bindings (not discovery_bindings).
-- Do not INSERT unlock keys from this file.

SELECT b.unlock_key, b.location_id, b.artifact_id
FROM discovery_content_bindings b
WHERE b.location_id = '11111111-1111-1111-1111-111111111111'
LIMIT 50;

SELECT discovery_unlock_key, artifact_unlock_key, location_id
FROM discovery_artifact_links
WHERE location_id = '11111111-1111-1111-1111-111111111111'
LIMIT 50;
