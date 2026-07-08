INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('chat_free_daily_limit', '10', NOW())
ON CONFLICT (setting_key) DO NOTHING;
