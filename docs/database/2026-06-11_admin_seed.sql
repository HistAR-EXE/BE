-- Platform admin account (dev/demo only — change password on production)
-- Same password hash as seed users in TimeLens_DB_Schema.sql (register new password in prod)

INSERT INTO profiles (email, password_hash, provider, role, display_name, avatar_url, level, total_points, city, created_at)
VALUES (
    'admin@histar.vn',
    '$2a$10$uQfV2m4K1S8mJjW9n8u9fezQ3L4kE0cM9Qz5bQ2QByC2eQ6HgG5Qe',
    'local',
    'ADMIN',
    'Platform Admin',
    NULL,
    1,
    0,
    'TP.HCM',
    now()
)
ON CONFLICT (email) DO UPDATE SET role = EXCLUDED.role;
