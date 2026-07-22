-- Production-ready monetization migration baseline.
-- Keeps existing data, enables Flyway on non-empty databases,
-- and converts B2C subscriptions to support real history.

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS contact_email VARCHAR(255);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_type VARCHAR(20) DEFAULT 'MICRO';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'ACTIVE';
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ccu INT DEFAULT 15;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_verified_accounts INT DEFAULT 100;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS max_ai_queries_per_month INT DEFAULT 5000;

UPDATE organizations SET plan_type = 'MICRO' WHERE plan_type IS NULL;
UPDATE organizations SET plan_type = 'STANDARD' WHERE LOWER(COALESCE(plan, '')) IN ('org_pro', 'pro', 'standard');
UPDATE organizations SET plan_type = 'MICRO' WHERE LOWER(COALESCE(plan, '')) IN ('org_basic', 'basic', 'trial', 'micro');
UPDATE organizations
SET max_ccu = 40, max_verified_accounts = 400, max_ai_queries_per_month = 30000
WHERE plan_type = 'STANDARD'
  AND (max_ccu IS NULL OR max_verified_accounts IS NULL OR max_ai_queries_per_month IS NULL);
UPDATE organizations
SET max_ccu = 80, max_verified_accounts = 1000, max_ai_queries_per_month = NULL
WHERE plan_type = 'PREMIUM'
  AND (max_ccu IS NULL OR max_verified_accounts IS NULL);

CREATE TABLE IF NOT EXISTS b2c_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id),
    price_vnd INT NOT NULL DEFAULT 79000,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

UPDATE b2c_subscriptions
SET created_at = NOW()
WHERE created_at IS NULL;

DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    SELECT tc.constraint_name
    INTO constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.constraint_column_usage ccu
      ON tc.constraint_name = ccu.constraint_name
     AND tc.table_schema = ccu.table_schema
    WHERE tc.table_schema = 'public'
      AND tc.table_name = 'b2c_subscriptions'
      AND tc.constraint_type = 'UNIQUE'
      AND ccu.column_name = 'user_id'
    LIMIT 1;

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE public.b2c_subscriptions DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

DROP INDEX IF EXISTS uq_b2c_subscriptions_user_id;
DROP INDEX IF EXISTS b2c_subscriptions_user_id_key;

CREATE INDEX IF NOT EXISTS idx_b2c_subscriptions_user_created_at
    ON b2c_subscriptions (user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_subscriptions_active_user
    ON b2c_subscriptions (user_id)
    WHERE is_active IS TRUE;

CREATE TABLE IF NOT EXISTS subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    plan_type VARCHAR(20) NOT NULL,
    price_vnd BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

UPDATE subscriptions
SET created_at = NOW()
WHERE created_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_subscriptions_org_created_at
    ON subscriptions (organization_id, created_at DESC);

CREATE TABLE IF NOT EXISTS usage_quotas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID REFERENCES organizations(id),
    user_id UUID REFERENCES profiles(id),
    year INT NOT NULL,
    month INT NOT NULL,
    day INT,
    used_ai_queries INT NOT NULL DEFAULT 0,
    CONSTRAINT usage_quotas_scope_chk CHECK (
        (organization_id IS NOT NULL AND user_id IS NULL)
        OR (organization_id IS NULL AND user_id IS NOT NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_org_month
    ON usage_quotas (organization_id, year, month)
    WHERE organization_id IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_usage_quotas_user_day
    ON usage_quotas (user_id, year, month, day)
    WHERE user_id IS NOT NULL AND day IS NOT NULL;
