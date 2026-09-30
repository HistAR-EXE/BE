-- E2: FCM device tokens (one row per user + platform)

CREATE TABLE IF NOT EXISTS device_push_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL REFERENCES profiles (id) ON DELETE CASCADE,
    token      VARCHAR(512) NOT NULL,
    platform   VARCHAR(16)  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_push_tokens_user_platform UNIQUE (user_id, platform),
    CONSTRAINT chk_device_push_platform CHECK (platform IN ('ANDROID', 'IOS', 'WEB'))
);

CREATE INDEX IF NOT EXISTS idx_device_push_tokens_user ON device_push_tokens (user_id);
