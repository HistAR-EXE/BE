-- V33: Journey Pass (72h visit entitlement) + locations.site_code for multi-site pilot registry.

ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);

UPDATE locations SET site_code = 'cu-chi'
WHERE id = '11111111-1111-1111-1111-111111111111' AND (site_code IS NULL OR site_code = '');

UPDATE locations SET site_code = 'hoang-thanh-thang-long'
WHERE id = '22222222-2222-2222-2222-222222222204' AND (site_code IS NULL OR site_code = '');

UPDATE locations SET site_code = 'dai-noi-hue'
WHERE id = '22222222-2222-2222-2222-222222222208' AND (site_code IS NULL OR site_code = '');

CREATE UNIQUE INDEX IF NOT EXISTS uq_locations_site_code
    ON locations (site_code)
    WHERE site_code IS NOT NULL AND site_code <> '';

-- B2C Journey Pass price (default 29.000đ)
INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('b2c_journey_pass_price_vnd', '29000', NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- Visit entitlements: unlock story ch.3–6 + chat citations for one site within a window
CREATE TABLE IF NOT EXISTS b2c_visit_entitlements (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    site_code    VARCHAR(64)  NOT NULL,
    source       VARCHAR(32)  NOT NULL DEFAULT 'SEPAY',
    order_code   VARCHAR(64),
    starts_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    expires_at   TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_b2c_visit_entitlements_site CHECK (site_code ~ '^[a-z0-9][a-z0-9-]{0,63}$')
);

CREATE INDEX IF NOT EXISTS idx_b2c_visit_entitlements_user_site
    ON b2c_visit_entitlements (user_id, site_code, expires_at DESC);

-- Optional plan marker on B2C payment intents (PREMIUM vs JOURNEY_PASS)
ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(32) NOT NULL DEFAULT 'PREMIUM';

ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS site_code VARCHAR(64);
