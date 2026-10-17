-- Admin Organizations P0: contracts, AR cards, certificates, CRM link

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS last_crm_synced_at TIMESTAMPTZ;

ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS crm_linked_inquiry_count INT NOT NULL DEFAULT 0;

ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id);

CREATE INDEX IF NOT EXISTS idx_heritage_digitization_inquiries_org
    ON heritage_digitization_inquiries (organization_id);

CREATE TABLE IF NOT EXISTS organization_contracts (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    contract_code VARCHAR(64) NOT NULL,
    license_seats INT,
    plan_type VARCHAR(32),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    starts_on DATE,
    ends_on DATE,
    document_url TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID REFERENCES profiles(id),
    CONSTRAINT uq_org_contracts_org_code UNIQUE (organization_id, contract_code),
    CONSTRAINT chk_org_contracts_status CHECK (UPPER(status) IN ('DRAFT', 'ACTIVE', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_organization_contracts_org
    ON organization_contracts (organization_id);

CREATE TABLE IF NOT EXISTS organization_ar_cards (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    card_code VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    issued_by UUID REFERENCES profiles(id),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_org_ar_cards_status CHECK (UPPER(status) IN ('ACTIVE', 'REVOKED'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_org_ar_cards_active_member
    ON organization_ar_cards (organization_id, user_id)
    WHERE UPPER(status) = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_organization_ar_cards_org
    ON organization_ar_cards (organization_id);

CREATE TABLE IF NOT EXISTS organization_certificates (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    certificate_code VARCHAR(64) NOT NULL UNIQUE,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    issued_by UUID REFERENCES profiles(id),
    metadata_json TEXT
);

CREATE INDEX IF NOT EXISTS idx_organization_certificates_org
    ON organization_certificates (organization_id);

-- Optional demo contracts for first two orgs (idempotent by code)
INSERT INTO organization_contracts (
    id, organization_id, title, contract_code, license_seats, plan_type, status,
    starts_on, ends_on, notes, created_at, updated_at
)
SELECT
    gen_random_uuid(),
    o.id,
    'Hợp đồng trải nghiệm AR HistAR',
    'CTR-DEMO-' || UPPER(SUBSTRING(REPLACE(o.id::text, '-', ''), 1, 6)),
    COALESCE(o.max_verified_accounts, 30),
    COALESCE(o.plan_type, o.plan, 'STANDARD'),
    'ACTIVE',
    CURRENT_DATE - 30,
    CURRENT_DATE + 335,
    'Seed hợp đồng demo (V75)',
    NOW(),
    NOW()
FROM organizations o
WHERE NOT EXISTS (
    SELECT 1 FROM organization_contracts c
    WHERE c.organization_id = o.id AND c.contract_code LIKE 'CTR-DEMO-%'
)
ORDER BY o.created_at NULLS LAST, o.name
LIMIT 2;
