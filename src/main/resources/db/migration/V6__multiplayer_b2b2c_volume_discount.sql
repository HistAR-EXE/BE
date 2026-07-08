-- V6: Multiplayer org teams, B2B2C inquiries, volume discount settings

ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS org_id UUID;
ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS quest_id UUID;
ALTER TABLE study_groups ADD COLUMN IF NOT EXISTS team_mode VARCHAR(24) DEFAULT 'QUEST';

CREATE INDEX IF NOT EXISTS idx_study_groups_org_id ON study_groups(org_id);
CREATE INDEX IF NOT EXISTS idx_study_groups_quest_id ON study_groups(quest_id);

CREATE TABLE IF NOT EXISTS heritage_digitization_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_name VARCHAR(255) NOT NULL,
    contact_name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(64),
    package_type VARCHAR(32) NOT NULL,
    message TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('org_volume_discount_percent', '35', NOW())
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO billing_settings (setting_key, setting_value, updated_at)
VALUES ('org_volume_discount_min_licenses', '3', NOW())
ON CONFLICT (setting_key) DO NOTHING;
