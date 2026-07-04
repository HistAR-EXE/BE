-- Seed demo B2B org membership (cu-chi-demo) + teacher account
INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, tier, created_at)
VALUES (
    'teacher@histar.vn',
    '$2a$10$pNehDXSRLXQgtSVewDj/guZ1jaOBusRg5fj0UUguwTrZwchjZOlgG',
    'local',
    'TEACHER',
    'Giáo viên Demo',
    NULL,
    1,
    0,
    'TP.HCM',
    'FREE',
    now()
)
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role;

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    p.id,
    'teacher',
    now(),
    now()
FROM profiles p
WHERE p.email = 'teacher@histar.vn'
ON CONFLICT (organization_id, user_id) DO UPDATE SET org_role = EXCLUDED.org_role;

INSERT INTO organization_members (organization_id, user_id, org_role, invited_at, accepted_at)
SELECT
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    p.id,
    'student',
    now(),
    now()
FROM profiles p
WHERE p.role = 'USER'
  AND p.email NOT IN ('teacher@histar.vn')
ORDER BY p.created_at
LIMIT 5
ON CONFLICT (organization_id, user_id) DO NOTHING;
