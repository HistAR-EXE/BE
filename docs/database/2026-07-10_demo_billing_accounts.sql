-- Demo billing accounts (dev/local/Render seed) — password: Demo@2026
-- 1) premium@histar.vn       — B2C Premium (tier + active subscription, no expiry practical)
-- 2) org-standard@histar.vn  — B2B Standard org owner (ACTIVE org + subscription)

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organizations(id),
    ADD COLUMN IF NOT EXISTS org_subscription VARCHAR(16) DEFAULT 'NONE';

-- ---------------------------------------------------------------------------
-- B2C Premium
-- ---------------------------------------------------------------------------
INSERT INTO profiles (
    id, email, password_hash, provider, role, tier, display_name,
    level, total_points, city, email_verified, email_verified_at, created_at
)
VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'premium@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'USER',
    'PREMIUM',
    'Premium Demo',
    1,
    0,
    'TP.HCM',
    TRUE,
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'USER',
    tier = 'PREMIUM',
    org_id = NULL,
    org_subscription = 'NONE',
    email_verified = TRUE,
    email_verified_at = COALESCE(profiles.email_verified_at, now()),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

UPDATE b2c_subscriptions
SET is_active = FALSE
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- id phải set tường minh: một số DB tạo bảng không có DEFAULT gen_random_uuid()
INSERT INTO b2c_subscriptions (id, user_id, price_vnd, start_date, end_date, is_active, payment_method, created_at)
SELECT
    gen_random_uuid(),
    p.id,
    79000,
    CURRENT_DATE,
    DATE '2099-12-31',
    TRUE,
    'DEMO',
    now()
FROM profiles p
WHERE p.email = 'premium@histar.vn'
  AND NOT EXISTS (
      SELECT 1
      FROM b2c_subscriptions s
      WHERE s.user_id = p.id
        AND s.is_active = TRUE
  );

UPDATE b2c_subscriptions
SET end_date = DATE '2099-12-31',
    is_active = TRUE,
    payment_method = 'DEMO'
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- ---------------------------------------------------------------------------
-- B2B Standard org + owner
-- ---------------------------------------------------------------------------
INSERT INTO organizations (
    id, slug, name, plan, plan_type, contact_email, status,
    max_ccu, max_verified_accounts, max_ai_queries_per_month,
    plan_start_date, plan_end_date, created_at
)
VALUES (
    'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    'demo-standard-org',
    'Demo Standard Org',
    'standard',
    'STANDARD',
    'org-standard@histar.vn',
    'ACTIVE',
    40,
    400,
    30000,
    CURRENT_DATE,
    DATE '2099-12-31',
    now()
)
ON CONFLICT (slug) DO UPDATE SET
    name = EXCLUDED.name,
    plan = EXCLUDED.plan,
    plan_type = EXCLUDED.plan_type,
    contact_email = EXCLUDED.contact_email,
    status = EXCLUDED.status,
    max_ccu = EXCLUDED.max_ccu,
    max_verified_accounts = EXCLUDED.max_verified_accounts,
    max_ai_queries_per_month = EXCLUDED.max_ai_queries_per_month,
    plan_start_date = EXCLUDED.plan_start_date,
    plan_end_date = EXCLUDED.plan_end_date;

INSERT INTO profiles (
    id, email, password_hash, provider, role, tier, display_name,
    org_id, org_subscription, level, total_points, city,
    email_verified, email_verified_at, created_at
)
SELECT
    'd2222222-2222-2222-2222-222222222222'::uuid,
    'org-standard@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'TEACHER',
    'FREE',
    'Org Standard Demo',
    o.id,
    'STANDARD',
    1,
    0,
    'TP.HCM',
    TRUE,
    now(),
    now()
FROM organizations o
WHERE o.slug = 'demo-standard-org'
ON CONFLICT (email) DO UPDATE SET
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'TEACHER',
    tier = 'FREE',
    org_id = EXCLUDED.org_id,
    org_subscription = 'STANDARD',
    email_verified = TRUE,
    email_verified_at = COALESCE(profiles.email_verified_at, now()),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    o.id,
    p.id,
    'teacher',
    now(),
    now()
FROM profiles p
JOIN organizations o ON o.slug = 'demo-standard-org'
WHERE p.email = 'org-standard@histar.vn'
ON CONFLICT (organization_id, user_id) DO UPDATE SET
    org_role = EXCLUDED.org_role,
    accepted_at = COALESCE(organization_members.accepted_at, EXCLUDED.accepted_at);

UPDATE subscriptions
SET is_active = FALSE
WHERE organization_id = (SELECT id FROM organizations WHERE slug = 'demo-standard-org')
  AND is_active = TRUE;

INSERT INTO subscriptions (
    id, organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method, created_at
)
SELECT
    gen_random_uuid(),
    o.id,
    'STANDARD',
    15000000,
    CURRENT_DATE,
    DATE '2099-12-31',
    TRUE,
    'DEMO',
    now()
FROM organizations o
WHERE o.slug = 'demo-standard-org'
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      WHERE s.organization_id = o.id
        AND s.is_active = TRUE
  );

UPDATE subscriptions
SET end_date = DATE '2099-12-31',
    is_active = TRUE,
    payment_method = 'DEMO',
    plan_type = 'STANDARD'
WHERE organization_id = (SELECT id FROM organizations WHERE slug = 'demo-standard-org')
  AND is_active = TRUE;

COMMIT;
