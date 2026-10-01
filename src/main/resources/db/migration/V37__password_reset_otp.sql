-- Password reset via email OTP (local email/password accounts)

CREATE TABLE IF NOT EXISTS password_reset_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    otp_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    verified_at TIMESTAMPTZ,
    reset_token_hash VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_password_reset_challenges_user_active
    ON password_reset_challenges(user_id, created_at DESC)
    WHERE consumed_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_password_reset_challenges_reset_token
    ON password_reset_challenges(reset_token_hash)
    WHERE reset_token_hash IS NOT NULL;
