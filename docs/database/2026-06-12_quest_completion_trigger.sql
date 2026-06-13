-- Quest completion trigger for check-in evaluator
BEGIN;

ALTER TABLE quests ADD COLUMN IF NOT EXISTS completion_trigger VARCHAR(32) NOT NULL DEFAULT 'checkin';

UPDATE quests SET completion_trigger = 'checkin' WHERE completion_trigger IS NULL;

COMMIT;
