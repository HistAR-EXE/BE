-- Quest onsite requirement flag (BR-12)
ALTER TABLE quests
    ADD COLUMN IF NOT EXISTS require_onsite_checkin BOOLEAN NOT NULL DEFAULT false;

UPDATE quests
SET require_onsite_checkin = true
WHERE LOWER(completion_trigger) = 'checkin';
