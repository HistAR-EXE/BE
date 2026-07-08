-- Email verification + optional Firebase UID (Phase 2)

ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);

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

-- Seed / admin accounts treated as verified
UPDATE profiles
SET email_verified = TRUE,
    email_verified_at = COALESCE(email_verified_at, now())
WHERE email IN ('admin@histar.vn', 'demo@histar.vn', 'teacher@histar.vn');
