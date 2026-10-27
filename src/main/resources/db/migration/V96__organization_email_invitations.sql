-- Email-bound invitations: organization Premium is derived, never persisted as personal tier.
CREATE TABLE organization_email_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    invited_by UUID NOT NULL REFERENCES profiles(id),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    delivery_status VARCHAR(16) NOT NULL DEFAULT 'LOCAL_ONLY',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    CONSTRAINT uq_org_email_invitation UNIQUE (organization_id, email),
    CONSTRAINT ck_org_invitation_status CHECK (status IN ('PENDING','ACCEPTED','EXPIRED')),
    CONSTRAINT ck_org_invitation_delivery CHECK (delivery_status IN ('SENT','LOCAL_ONLY')),
    CONSTRAINT ck_org_invitation_email CHECK (email = lower(btrim(email)))
);
CREATE INDEX idx_org_invitation_pending_email ON organization_email_invitations(email) WHERE status = 'PENDING';
