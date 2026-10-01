-- Ensure platform admin accounts have role ADMIN and known demo password.
-- Run after self-registration may have created these emails as USER.

UPDATE profiles
SET role = 'ADMIN',
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    display_name = COALESCE(NULLIF(display_name, ''), 'Platform Admin')
WHERE email = 'admin@histar.vn';

INSERT INTO profiles (email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at)
VALUES (
    'demo@histar.vn',
    crypt('Demo@2026', gen_salt('bf', 10)),
    'local',
    'ADMIN',
    'FREE',
    'Demo Admin',
    10,
    999,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET
    role = 'ADMIN',
    password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    display_name = COALESCE(EXCLUDED.display_name, profiles.display_name);

UPDATE profiles
SET role = 'ADMIN'
WHERE email IN ('admin@histar.vn', 'demo@histar.vn');

-- Demo teacher account (E2E role-ui tests)
UPDATE profiles
SET password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'TEACHER',
    display_name = COALESCE(NULLIF(display_name, ''), 'Giáo viên Demo')
WHERE email = 'teacher@histar.vn';
