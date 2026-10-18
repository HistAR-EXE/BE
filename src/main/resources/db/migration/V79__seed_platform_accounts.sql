-- Seed platform accounts (admin, demo, teacher) via Flyway so they survive DB resets.
-- Uses pgcrypto crypt() with bcrypt (bf, cost 10) to hash Demo@2026.
-- Idempotent: ON CONFLICT DO UPDATE ensures re-runnable.
-- These accounts are dev/demo only — rotate passwords on production.

-- Ensure pgcrypto extension is available (needed for crypt + gen_salt)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO profiles (email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at, email_verified, email_verified_at)
VALUES
    ('admin@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'ADMIN', 'FREE', 'Platform Admin',
     1, 0, 'TP.HCM', now(), true, now()),
    ('demo@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'ADMIN', 'FREE', 'Demo Admin',
     10, 999, 'TP.HCM', now(), true, now()),
    ('teacher@histar.vn',
     crypt('Demo@2026', gen_salt('bf', 10)),
     'local', 'TEACHER', 'FREE', 'Giáo viên Demo',
     1, 0, 'TP.HCM', now(), true, now())
ON CONFLICT (email) DO UPDATE
    SET password_hash      = crypt('Demo@2026', gen_salt('bf', 10)),
        role               = EXCLUDED.role,
        email_verified     = TRUE,
        email_verified_at  = COALESCE(profiles.email_verified_at, now());
