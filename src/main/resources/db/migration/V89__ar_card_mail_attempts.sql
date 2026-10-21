CREATE TABLE IF NOT EXISTS ar_card_mail_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    request_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    state VARCHAR(16) NOT NULL CHECK (state IN ('CREATED','ACCEPTED','UNKNOWN')),
    CONSTRAINT uq_ar_card_mail_user_request UNIQUE (user_id, request_id)
);
CREATE INDEX IF NOT EXISTS idx_ar_card_mail_user_created ON ar_card_mail_attempts(user_id, created_at DESC);
