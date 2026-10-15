-- Demo revenue for Admin dashboard chart (Stitch-like 6-month B2C/B2B).
-- Idempotent: keyed by order_code / transfer_content prefix SEED-DASH-*.
-- Depends on: 2026-07-10_demo_billing_accounts.sql (premium@histar.vn, demo-standard-org).
-- Amounts approximate Stitch wireframe (T5–T10) so the dashboard bar chart is visually readable.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure payer profiles/org exist (no-op if already seeded).
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM profiles WHERE email = 'premium@histar.vn') THEN
    RAISE EXCEPTION 'Missing premium@histar.vn — run 2026-07-10_demo_billing_accounts.sql first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM organizations WHERE slug = 'demo-standard-org') THEN
    RAISE EXCEPTION 'Missing demo-standard-org — run 2026-07-10_demo_billing_accounts.sql first';
  END IF;
END $$;

-- Paid B2C subscription so MRR/ARR cards are non-zero (DEMO method is excluded from MRR).
UPDATE b2c_subscriptions
SET payment_method = 'SEPAY',
    price_vnd = 79000,
    is_active = TRUE,
    end_date = DATE '2099-12-31'
WHERE user_id = (SELECT id FROM profiles WHERE email = 'premium@histar.vn')
  AND is_active = TRUE;

-- Extra paid B2C subs for MRR demo volume (synthetic users optional — skip if not present).
-- Keep focused on payment transactions for the chart.

DELETE FROM b2c_payment_transactions WHERE order_code LIKE 'SEED-DASH-B2C-%';
DELETE FROM org_payment_transactions WHERE order_code LIKE 'SEED-DASH-B2B-%';

-- Month offsets relative to Asia/Ho_Chi_Minh "now": 5..0 months ago.
-- Targets (VND): B2C [520M, 610M, 780M, 690M, 890M, 1025M], B2B [350M, 430M, 560M, 650M, 760M, 820200000]
WITH months AS (
  SELECT * FROM (VALUES
    (5, 520000000, 350000000),
    (4, 610000000, 430000000),
    (3, 780000000, 560000000),
    (2, 690000000, 650000000),
    (1, 890000000, 760000000),
    (0, 1025000000, 820200000)
  ) AS t(offset_months, b2c_vnd, b2b_vnd)
),
payer AS (
  SELECT id AS user_id FROM profiles WHERE email = 'premium@histar.vn' LIMIT 1
),
org AS (
  SELECT o.id AS organization_id, p.id AS requester_user_id
  FROM organizations o
  JOIN profiles p ON p.email = 'org-standard@histar.vn'
  WHERE o.slug = 'demo-standard-org'
  LIMIT 1
)
INSERT INTO b2c_payment_transactions (
  id, user_id, provider, order_code, transfer_content, amount_vnd, status,
  return_to_path, expires_at, paid_at, created_at, updated_at, provider_gateway
)
SELECT
  gen_random_uuid(),
  payer.user_id,
  'SEPAY',
  'SEED-DASH-B2C-' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYY-MM'),
  'SEEDDASHB2C' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYYMM'),
  m.b2c_vnd,
  'PAID',
  '/billing',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days' + interval '10 hours',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '1 day',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days' + interval '10 hours',
  'SEED'
FROM months m
CROSS JOIN payer;

WITH months AS (
  SELECT * FROM (VALUES
    (5, 520000000, 350000000),
    (4, 610000000, 430000000),
    (3, 780000000, 560000000),
    (2, 690000000, 650000000),
    (1, 890000000, 760000000),
    (0, 1025000000, 820200000)
  ) AS t(offset_months, b2c_vnd, b2b_vnd)
),
org_edu AS (
  SELECT o.id AS organization_id, p.id AS requester_user_id, o.name AS org_name
  FROM organizations o
  JOIN profiles p ON p.email = 'org-standard@histar.vn'
  WHERE o.slug = 'demo-standard-org'
  LIMIT 1
),
org_museum AS (
  SELECT o.id AS organization_id,
         COALESCE(
           (SELECT id FROM profiles WHERE email = 'org-basic@histar.vn' LIMIT 1),
           (SELECT id FROM profiles WHERE email = 'org-standard@histar.vn' LIMIT 1)
         ) AS requester_user_id,
         o.name AS org_name
  FROM organizations o
  WHERE o.slug = 'cu-chi-demo'
  LIMIT 1
)
-- ~55% B2B as Edu (STANDARD), ~45% as Museum/Org (ORG_BASIC) for planRevenue donut.
INSERT INTO org_payment_transactions (
  id, requester_user_id, organization_id, org_name, contact_email, plan_type, provider,
  order_code, transfer_content, amount_vnd, status, return_to_path, expires_at, paid_at,
  created_at, updated_at, provider_gateway
)
SELECT
  gen_random_uuid(),
  CASE WHEN m.offset_months % 2 = 0 THEN edu.requester_user_id ELSE museum.requester_user_id END,
  CASE WHEN m.offset_months % 2 = 0 THEN edu.organization_id ELSE museum.organization_id END,
  CASE WHEN m.offset_months % 2 = 0 THEN edu.org_name ELSE museum.org_name END,
  CASE WHEN m.offset_months % 2 = 0 THEN 'org-standard@histar.vn' ELSE 'org-basic@histar.vn' END,
  CASE WHEN m.offset_months % 2 = 0 THEN 'STANDARD' ELSE 'ORG_BASIC' END,
  'SEPAY',
  'SEED-DASH-B2B-' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYY-MM'),
  'SEEDDASHB2B' || to_char((date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval), 'YYYYMM'),
  m.b2b_vnd,
  'PAID',
  '/billing',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '4 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days' + interval '14 hours',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '2 days',
  (date_trunc('month', now() AT TIME ZONE 'Asia/Ho_Chi_Minh') - (m.offset_months || ' months')::interval) + interval '3 days' + interval '14 hours',
  'SEED'
FROM months m
CROSS JOIN org_edu edu
CROSS JOIN org_museum museum;

-- Sample audit events for dashboard ops log (optional visual).
DELETE FROM admin_audit_events WHERE idempotency_key LIKE 'seed-dash-ops-%';

INSERT INTO admin_audit_events (
  id, actor_id, entity_type, entity_id, action, reason, metadata_json, occurred_at, idempotency_key
)
SELECT
  gen_random_uuid(),
  COALESCE(
    (SELECT id FROM profiles WHERE email IN ('admin@histar.vn', 'demo-admin@histar.vn') ORDER BY email LIMIT 1),
    (SELECT id FROM profiles WHERE email = 'premium@histar.vn' LIMIT 1)
  ),
  v.entity_type,
  v.entity_id::uuid,
  v.action,
  v.reason,
  v.metadata,
  now() - v.ago,
  v.idem
FROM (VALUES
  ('QUEST', 'a1111111-1111-4111-8111-111111111111', 'CREATED', 'Đơn hàng quest demo seed', '{"channel":"VietQR","amountVnd":380000}', interval '5 minutes', 'seed-dash-ops-1'),
  ('ORGANIZATION', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'UPDATED', 'Nạp credit đối tác B2B demo', '{"amountVnd":25000000}', interval '12 minutes', 'seed-dash-ops-2'),
  ('NARRATIVE_INVESTIGATION', 'c1111111-2222-4333-8444-555555555555', 'REVIEWED', 'Phê duyệt Pilot mới (seed)', '{"by":"admin"}', interval '45 minutes', 'seed-dash-ops-3'),
  ('DISCOVERY_POINT', 'd1111111-2222-4333-8444-666666666666', 'UPDATED', 'Khiếu nại sử liệu trạm #04', '{"status":"OPEN"}', interval '1 hour', 'seed-dash-ops-4'),
  ('ORGANIZATION', 'e1111111-2222-4333-8444-777777777777', 'CREATED', 'Tổ chức đăng ký mới (seed)', '{"name":"Bảo tàng Điêu khắc Chăm"}', interval '2 hours', 'seed-dash-ops-5')
) AS v(entity_type, entity_id, action, reason, metadata, ago, idem);

COMMIT;
