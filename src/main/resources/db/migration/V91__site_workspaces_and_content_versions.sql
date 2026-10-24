-- B2G site workspaces, representative onboarding, and reviewed content snapshots.

-- V33 used a partial unique index for non-empty site codes. PostgreSQL foreign keys
-- require a non-partial unique key on the referenced columns; UNIQUE still permits
-- multiple NULL values while preserving the existing non-null uniqueness contract.
UPDATE locations SET site_code = NULL WHERE site_code = '';

ALTER TABLE locations
    ADD CONSTRAINT uq_locations_site_code_reference UNIQUE (site_code);

CREATE TABLE site_memberships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code VARCHAR(64) NOT NULL REFERENCES locations(site_code) ON UPDATE CASCADE ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    role VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    granted_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_site_memberships_site_user UNIQUE (site_code, user_id),
    CONSTRAINT chk_site_memberships_role
        CHECK (role IN ('SITE_MANAGER', 'CONTENT_EDITOR', 'CONTENT_REVIEWER'))
);

CREATE INDEX idx_site_memberships_user_active
    ON site_memberships (user_id, active);

CREATE INDEX idx_site_memberships_site_active
    ON site_memberships (site_code, active);

CREATE TABLE site_onboarding_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requester_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    site_code VARCHAR(64) NOT NULL REFERENCES locations(site_code) ON UPDATE CASCADE ON DELETE CASCADE,
    representative_name VARCHAR(255) NOT NULL,
    organization_name VARCHAR(255) NOT NULL,
    representative_email VARCHAR(320) NOT NULL,
    evidence_note TEXT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_site_onboarding_requests_status
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

CREATE INDEX idx_site_onboarding_requests_site_status
    ON site_onboarding_requests (site_code, status);

CREATE INDEX idx_site_onboarding_requests_requester_created
    ON site_onboarding_requests (requester_id, created_at DESC);

CREATE TABLE site_workspace_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code VARCHAR(64) NOT NULL REFERENCES locations(site_code) ON UPDATE CASCADE ON DELETE CASCADE,
    requires_trip_confirmation BOOLEAN NOT NULL DEFAULT FALSE,
    updated_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_site_workspace_settings_site UNIQUE (site_code)
);

CREATE TABLE site_content_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_code VARCHAR(64) NOT NULL REFERENCES locations(site_code) ON UPDATE CASCADE ON DELETE CASCADE,
    content_kind VARCHAR(32) NOT NULL,
    resource_key VARCHAR(128) NOT NULL,
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    station_id UUID REFERENCES stations(id) ON DELETE CASCADE,
    version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    snapshot_json JSONB NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL REFERENCES profiles(id),
    submitted_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    reviewed_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    published_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    review_note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at TIMESTAMPTZ,
    reviewed_at TIMESTAMPTZ,
    published_at TIMESTAMPTZ,
    CONSTRAINT uq_site_content_versions_resource_version
        UNIQUE (site_code, content_kind, resource_key, version),
    CONSTRAINT chk_site_content_versions_kind
        CHECK (content_kind IN ('SITE_PROFILE', 'STATION')),
    CONSTRAINT chk_site_content_versions_status
        CHECK (status IN ('DRAFT', 'IN_REVIEW', 'APPROVED', 'PUBLISHED', 'ARCHIVED')),
    CONSTRAINT chk_site_content_versions_resource_fk
        CHECK ((content_kind = 'SITE_PROFILE' AND location_id IS NOT NULL AND station_id IS NULL)
            OR (content_kind = 'STATION' AND location_id IS NULL AND station_id IS NOT NULL)),
    CONSTRAINT chk_site_content_versions_station_snapshot
        CHECK (content_kind <> 'STATION' OR (
            snapshot_json ?& ARRAY['stationCode', 'name', 'sortOrder', 'lat', 'lng', 'questStepKey', 'active', 'blocks']
            AND jsonb_typeof(snapshot_json -> 'blocks') = 'array'
        )),
    CONSTRAINT chk_site_content_versions_version CHECK (version > 0),
    CONSTRAINT chk_site_content_versions_revision CHECK (revision >= 0)
);

CREATE INDEX idx_site_content_versions_lookup
    ON site_content_versions (site_code, content_kind, resource_key, status);

CREATE INDEX idx_site_content_versions_station
    ON site_content_versions (station_id, version DESC)
    WHERE station_id IS NOT NULL;

CREATE INDEX idx_site_content_versions_location
    ON site_content_versions (location_id, version DESC)
    WHERE location_id IS NOT NULL;
