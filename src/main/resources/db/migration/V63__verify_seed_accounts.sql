-- Seeded platform accounts are controlled demo/test identities. They must not
-- enter the end-user email-verification flow after a database reset or reseed.
UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn')
  AND email_verified IS DISTINCT FROM TRUE;
