-- Gate 0 SePay entitlement hardening.
-- 1) B2C Premium price is 49.000đ/tháng (supersedes V16's 79.000đ).
-- 2) Link a paid SePay transaction to the b2c_subscriptions row it activated.
--    Status UNDERPAID is a code-level value on b2c_payment_transactions.status (VARCHAR), no schema change.

UPDATE billing_settings
SET setting_value = '49000',
    updated_at = NOW()
WHERE setting_key = 'b2c_premium_price_vnd';

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
SELECT 'b2c_premium_price_vnd', '49000', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM billing_settings WHERE setting_key = 'b2c_premium_price_vnd'
);

ALTER TABLE b2c_payment_transactions
    ADD COLUMN IF NOT EXISTS subscription_id UUID NULL REFERENCES b2c_subscriptions(id);

CREATE INDEX IF NOT EXISTS idx_b2c_payment_transactions_subscription_id
    ON b2c_payment_transactions (subscription_id)
    WHERE subscription_id IS NOT NULL;
