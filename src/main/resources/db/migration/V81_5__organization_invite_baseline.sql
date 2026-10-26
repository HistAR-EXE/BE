-- Organization invite fields historically came from Hibernate ddl-auto.
-- V82 seeds organizations using them, so Flyway-only databases need an
-- explicit baseline without modifying the historical V82 checksum.
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8),
    ADD COLUMN IF NOT EXISTS invite_code_expires_at TIMESTAMPTZ;

CREATE UNIQUE INDEX IF NOT EXISTS uq_organizations_invite_code
    ON organizations(invite_code)
    WHERE invite_code IS NOT NULL;

-- Profile organization linkage was also previously created by Hibernate.
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS org_subscription VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_profiles_org_id ON profiles(org_id);
