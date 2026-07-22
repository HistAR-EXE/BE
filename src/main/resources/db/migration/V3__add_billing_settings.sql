CREATE TABLE IF NOT EXISTS billing_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('b2c_premium_price_vnd', '79000', NOW())
ON CONFLICT (setting_key) DO NOTHING;
