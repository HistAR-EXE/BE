-- C4: Creator referral codes + landing visit tracking.
CREATE TABLE IF NOT EXISTS referral_codes (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(64)  NOT NULL UNIQUE,
    creator_name  VARCHAR(255) NOT NULL,
    headline      VARCHAR(255),
    active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS referral_visits (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    referral_code  VARCHAR(64)  NOT NULL REFERENCES referral_codes (code) ON DELETE CASCADE,
    user_id        UUID,
    session_id     VARCHAR(64),
    visited_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_referral_visits_code ON referral_visits (referral_code, visited_at DESC);

INSERT INTO referral_codes (id, code, creator_name, headline) VALUES
('c4000001-0000-4000-8000-000000000001', 'demo', 'HistAR Demo Creator', 'Khám phá địa đạo Củ Chi cùng HistAR')
ON CONFLICT (code) DO NOTHING;
