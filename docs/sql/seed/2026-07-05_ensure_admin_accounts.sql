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

-- This seed can be re-run after Flyway has added email verification columns.
-- Use dynamic SQL so the same file also remains compatible with the pristine
-- bootstrap schema where those columns do not exist until V9.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'profiles'
          AND column_name = 'email_verified'
    ) THEN
        EXECUTE $sql$
            UPDATE profiles
            SET email_verified = TRUE,
                email_verified_at = COALESCE(email_verified_at, now())
            WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn')
        $sql$;
    END IF;
END $$;

-- Demo teacher account (E2E role-ui tests)
UPDATE profiles
SET password_hash = crypt('Demo@2026', gen_salt('bf', 10)),
    role = 'TEACHER',
    display_name = COALESCE(NULLIF(display_name, ''), 'Giáo viên Demo')
WHERE email = 'teacher@histar.vn';
