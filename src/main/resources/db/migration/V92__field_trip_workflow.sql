-- Field trips connect an organization visit to a site, quest, and optional campaign.
-- Report JSON is opaque at the schema layer; excluding learner PII is the report writer's job.

CREATE TABLE field_trips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    org_id UUID NOT NULL REFERENCES organizations(id),
    site_code VARCHAR(64) NOT NULL REFERENCES locations(site_code) ON UPDATE CASCADE ON DELETE RESTRICT,
    title VARCHAR(255) NOT NULL,
    visit_start TIMESTAMPTZ NOT NULL,
    visit_end TIMESTAMPTZ NOT NULL,
    quest_id UUID NOT NULL REFERENCES quests(id),
    campaign_id UUID REFERENCES quest_campaigns(id),
    join_code VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    site_confirmation_status VARCHAR(32) NOT NULL DEFAULT 'NOT_REQUIRED',
    site_note TEXT,
    created_by UUID NOT NULL REFERENCES profiles(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    row_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_field_trips_visit_window CHECK (visit_end > visit_start),
    CONSTRAINT chk_field_trips_status
        CHECK (status IN ('DRAFT', 'PENDING_SITE', 'CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_field_trips_site_confirmation_status
        CHECK (site_confirmation_status IN ('NOT_REQUIRED', 'PENDING', 'CONFIRMED', 'DECLINED')),
    CONSTRAINT chk_field_trips_site_code_lowercase CHECK (site_code = LOWER(site_code)),
    CONSTRAINT chk_field_trips_site_code_canonical
        CHECK (site_code ~ '^[a-z0-9][a-z0-9-]{0,63}$')
);

CREATE UNIQUE INDEX uq_field_trips_join_code_ci ON field_trips (LOWER(join_code));

CREATE INDEX idx_field_trips_org_id ON field_trips (org_id);

CREATE INDEX idx_field_trips_site_code ON field_trips (site_code);

CREATE INDEX idx_field_trips_status ON field_trips (status);

ALTER TABLE study_groups
    ADD COLUMN field_trip_id UUID REFERENCES field_trips(id) ON DELETE SET NULL,
    ADD COLUMN participation_mode VARCHAR(32),
    ADD COLUMN declared_size INTEGER,
    ADD CONSTRAINT chk_study_groups_participation_mode
        CHECK (participation_mode IN ('INDIVIDUAL', 'SHARED_DEVICE')),
    ADD CONSTRAINT chk_study_groups_declared_size
        CHECK (declared_size IS NULL OR declared_size >= 1);

CREATE INDEX idx_study_groups_field_trip_id ON study_groups (field_trip_id);

ALTER TABLE assignments
    ADD COLUMN field_trip_id UUID REFERENCES field_trips(id) ON DELETE SET NULL;

CREATE UNIQUE INDEX uq_assignments_field_trip_id ON assignments (field_trip_id);

CREATE TABLE field_trip_report_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    field_trip_id UUID NOT NULL REFERENCES field_trips(id) ON DELETE CASCADE,
    generated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    report_json JSONB NOT NULL,
    CONSTRAINT uq_field_trip_report_snapshots_field_trip UNIQUE (field_trip_id)
);
