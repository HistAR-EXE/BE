-- Fix user_quest_progress rows missing current_step (caused start quest 500)
UPDATE user_quest_progress
SET current_step = 0
WHERE current_step IS NULL;
