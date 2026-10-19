-- Seed demo accounts for all 3 business segments (EXE201 demo readiness).
-- Password: Demo@2026 (bcrypt via pgcrypto).
-- Idempotent: ON CONFLICT DO UPDATE.
--
-- B2C Premium:  premium@histar.vn     (tier=PREMIUM, active b2c_subscription)
-- B2B Standard: org-standard@histar.vn (tier=FREE, org_id→Demo School Standard)
-- B2B Micro:    org-micro@histar.vn   (tier=FREE, org_id→Demo School Micro)
-- B2B Teacher:  teacher@histar.vn already exists (V79), updated with org link
-- Existing:     demo@histar.vn (ADMIN), admin@histar.vn (ADMIN) — kept as-is.
-- Mã mời: DEMOSTD1 (Standard), DEMOMIC1 (Micro) — organizations.invite_code là VARCHAR(8).

CREATE EXTENSION IF NOT EXISTS pgcrypto;

---------------------------------------------------------------------------
-- 1. B2C Premium user
---------------------------------------------------------------------------
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at)
VALUES ('premium@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'USER', 'PREMIUM', 'Khách Premium Demo',
        5, 350, 'TP.HCM', now(), true, now())
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        tier              = 'PREMIUM',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- Active B2C subscription for premium user (30-day window)
INSERT INTO b2c_subscriptions (user_id, price_vnd, start_date, end_date, is_active, payment_method)
SELECT p.id, 79000, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', TRUE, 'DEMO'
FROM profiles p
WHERE p.email = 'premium@histar.vn'
  AND NOT EXISTS (
    SELECT 1 FROM b2c_subscriptions bs
    WHERE bs.user_id = p.id AND bs.is_active = TRUE
);

---------------------------------------------------------------------------
-- 2. B2B Organizations (Standard + Micro)
---------------------------------------------------------------------------
-- Demo School Standard
INSERT INTO organizations (slug, name, plan, plan_type, contact_email,
                           plan_start_date, plan_end_date, status,
                           max_ccu, max_verified_accounts, max_ai_queries_per_month,
                           invite_code, invite_code_expires_at, created_at)
SELECT 'demo-school-standard', 'Trường THPT Demo Standard', 'org_standard', 'STANDARD',
       'org-standard@histar.vn',
       CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', 'ACTIVE',
       40, 400, 30000,
       'DEMOSTD1', now() + INTERVAL '365 days', now()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-school-standard');

UPDATE organizations
SET plan_type              = 'STANDARD',
    max_ccu                = 40,
    max_verified_accounts  = 400,
    max_ai_queries_per_month = 30000,
    plan_end_date          = CURRENT_DATE + INTERVAL '90 days',
    status                 = 'ACTIVE'
WHERE slug = 'demo-school-standard';

-- Demo School Micro
INSERT INTO organizations (slug, name, plan, plan_type, contact_email,
                           plan_start_date, plan_end_date, status,
                           max_ccu, max_verified_accounts, max_ai_queries_per_month,
                           invite_code, invite_code_expires_at, created_at)
SELECT 'demo-school-micro', 'Trường THPT Demo Micro', 'org_micro', 'MICRO',
       'org-micro@histar.vn',
       CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', 'ACTIVE',
       15, 100, 5000,
       'DEMOMIC1', now() + INTERVAL '365 days', now()
WHERE NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-school-micro');

UPDATE organizations
SET plan_type              = 'MICRO',
    max_ccu                = 15,
    max_verified_accounts  = 100,
    max_ai_queries_per_month = 5000,
    plan_end_date          = CURRENT_DATE + INTERVAL '30 days',
    status                 = 'ACTIVE'
WHERE slug = 'demo-school-micro';

---------------------------------------------------------------------------
-- 3. B2B Org member accounts
---------------------------------------------------------------------------
-- org-standard@histar.vn — org admin for Standard school
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at,
                      org_id, org_subscription)
VALUES ('org-standard@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'TEACHER', 'FREE', 'GV Demo Standard',
        3, 120, 'TP.HCM', now(), true, now(),
        (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
        'STANDARD')
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        role              = 'TEACHER',
        org_id            = (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
        org_subscription  = 'STANDARD',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- org-micro@histar.vn — org admin for Micro school
INSERT INTO profiles (email, password_hash, provider, role, tier, display_name,
                      level, total_points, city, created_at, email_verified, email_verified_at,
                      org_id, org_subscription)
VALUES ('org-micro@histar.vn',
        crypt('Demo@2026', gen_salt('bf', 10)),
        'local', 'TEACHER', 'FREE', 'GV Demo Micro',
        1, 0, 'TP.HCM', now(), true, now(),
        (SELECT id FROM organizations WHERE slug = 'demo-school-micro'),
        'MICRO')
ON CONFLICT (email) DO UPDATE
    SET password_hash     = crypt('Demo@2026', gen_salt('bf', 10)),
        role              = 'TEACHER',
        org_id            = (SELECT id FROM organizations WHERE slug = 'demo-school-micro'),
        org_subscription  = 'MICRO',
        email_verified    = TRUE,
        email_verified_at = COALESCE(profiles.email_verified_at, now());

-- Link existing teacher@histar.vn to the Standard org
UPDATE profiles
SET org_id           = (SELECT id FROM organizations WHERE slug = 'demo-school-standard'),
    org_subscription = 'STANDARD'
WHERE email = 'teacher@histar.vn'
  AND org_id IS NULL;

---------------------------------------------------------------------------
-- 4. B2B subscription records
---------------------------------------------------------------------------
INSERT INTO subscriptions (organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method)
SELECT o.id, 'STANDARD', 2490000, CURRENT_DATE, CURRENT_DATE + INTERVAL '90 days', TRUE, 'DEMO'
FROM organizations o
WHERE o.slug = 'demo-school-standard'
  AND NOT EXISTS (
    SELECT 1 FROM subscriptions s
    WHERE s.organization_id = o.id AND s.is_active = TRUE
);

INSERT INTO subscriptions (organization_id, plan_type, price_vnd, start_date, end_date, is_active, payment_method)
SELECT o.id, 'MICRO', 490000, CURRENT_DATE, CURRENT_DATE + INTERVAL '30 days', TRUE, 'DEMO'
FROM organizations o
WHERE o.slug = 'demo-school-micro'
  AND NOT EXISTS (
    SELECT 1 FROM subscriptions s
    WHERE s.organization_id = o.id AND s.is_active = TRUE
);
