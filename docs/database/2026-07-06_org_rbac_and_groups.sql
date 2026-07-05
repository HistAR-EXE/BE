-- Org RBAC + study groups (Phase C)

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS invite_code VARCHAR(8) UNIQUE;
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS invite_code_expires_at TIMESTAMPTZ;

ALTER TABLE profiles ADD COLUMN IF NOT EXISTS org_id UUID REFERENCES organizations(id);
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS org_subscription VARCHAR(16) DEFAULT 'NONE';

CREATE TABLE IF NOT EXISTS study_groups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(6) UNIQUE NOT NULL,
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE IF NOT EXISTS study_group_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL REFERENCES study_groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (group_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_study_group_members_user ON study_group_members(user_id);

UPDATE organizations
SET invite_code = 'CUCHI26A',
    invite_code_expires_at = now() + interval '90 days',
    plan = 'org_basic'
WHERE slug = 'cu-chi-demo' AND invite_code IS NULL;
