-- Sync local/manual seed DBs: B2C Premium 79.000đ/tháng
UPDATE billing_settings
SET setting_value = '79000',
    updated_at = NOW()
WHERE setting_key = 'b2c_premium_price_vnd';

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
SELECT 'b2c_premium_price_vnd', '79000', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM billing_settings WHERE setting_key = 'b2c_premium_price_vnd'
);
