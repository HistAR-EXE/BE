-- Discovery content bindings enrich (BR-09)
ALTER TABLE discovery_content_bindings
    ADD COLUMN IF NOT EXISTS artifact_id UUID NULL,
    ADD COLUMN IF NOT EXISTS xp_bonus INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS quest_step_id UUID NULL;

-- Backfill artifact_id from discovery_artifact_links when unlock_key matches
UPDATE discovery_content_bindings b
SET artifact_id = a.id
FROM discovery_artifact_links l
JOIN artifacts a ON a.unlock_key = l.artifact_unlock_key
WHERE l.discovery_unlock_key = b.unlock_key
  AND (b.location_id IS NULL OR b.location_id = l.location_id OR b.location_id = a.location_id)
  AND b.artifact_id IS NULL;

-- Default XP bonus for engagement bindings
UPDATE discovery_content_bindings
SET xp_bonus = 10
WHERE xp_bonus = 0 AND engagement IS NOT NULL;
