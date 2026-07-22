-- Raise default B2C Premium price from 49.000đ to 79.000đ/tháng.
-- Updates runtime billing_settings used by public pricing + SePay QR amount.

UPDATE billing_settings
SET setting_value = '79000',
    updated_at = NOW()
WHERE setting_key = 'b2c_premium_price_vnd'
  AND setting_value IN ('49000', '49_000');

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
SELECT 'b2c_premium_price_vnd', '79000', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM billing_settings WHERE setting_key = 'b2c_premium_price_vnd'
);
