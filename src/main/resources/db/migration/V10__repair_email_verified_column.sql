-- Repair partial schema when Hibernate ddl-auto ran before Flyway V9 completed.
-- Safe to run on DBs where V9 already applied (IF NOT EXISTS / idempotent updates).

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS email_verified BOOLEAN;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ;
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);

UPDATE profiles SET email_verified = FALSE WHERE email_verified IS NULL;

UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn');

ALTER TABLE profiles ALTER COLUMN email_verified SET DEFAULT FALSE;
ALTER TABLE profiles ALTER COLUMN email_verified SET NOT NULL;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user
    ON email_verification_tokens(user_id, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS idx_email_verification_tokens_hash
    ON email_verification_tokens(token_hash);
