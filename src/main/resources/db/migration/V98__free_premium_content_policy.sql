-- Keep historical progress and point ledger intact; change only future access metadata.
UPDATE billing_settings SET setting_value = '3', updated_at = NOW()
WHERE setting_key = 'chat_free_daily_limit' AND setting_value = '10';

UPDATE quests SET unlock_after_quest_id = NULL
WHERE id = '33333333-3333-3333-3333-333333333334'::uuid;

UPDATE quests SET
    title = 'Mật lệnh Mini Game',
    description = 'Hoàn thành bốn mini game để giải mật lệnh Củ Chi.',
    story = 'Khám phá từng dấu vết, vượt qua bốn thử thách và ghép lại mật lệnh.',
    points_reward = 100,
    sort_order = 55,
    track = 'ONLINE'
WHERE id = '33333333-3333-3333-3333-333333333336'::uuid;
