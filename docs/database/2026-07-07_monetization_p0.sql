-- Monetization P0: usage quotas, B2C/B2B subscriptions, org plan limits

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
UPDATE organizations SET max_ccu = 40, max_verified_accounts = 400, max_ai_queries_per_month = 30000
  WHERE plan_type = 'STANDARD';
UPDATE organizations SET max_ccu = 80, max_verified_accounts = 1000, max_ai_queries_per_month = NULL
  WHERE plan_type = 'PREMIUM';

CREATE TABLE IF NOT EXISTS b2c_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) UNIQUE,
    price_vnd INT NOT NULL DEFAULT 49000,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    payment_method VARCHAR(50) DEFAULT 'DEMO',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

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
