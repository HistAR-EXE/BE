CREATE TABLE narrative_investigation_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    version INTEGER NOT NULL,
    graph_json TEXT NOT NULL,
    review_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    approved_locales_json TEXT NOT NULL DEFAULT '[]',
    reviewed_by VARCHAR(160),
    reviewed_at TIMESTAMPTZ,
    revision BIGINT NOT NULL DEFAULT 0,
    open_draft BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_narrative_version_number UNIQUE (investigation_id, version),
    CONSTRAINT ck_narrative_version_review_status
        CHECK (review_status IN ('DRAFT', 'APPROVED', 'REJECTED'))
);

CREATE UNIQUE INDEX uq_narrative_open_draft
    ON narrative_investigation_versions(investigation_id)
    WHERE open_draft;

ALTER TABLE narrative_investigations
    ADD COLUMN published_version_id UUID,
    ADD COLUMN rollout_state VARCHAR(24) NOT NULL DEFAULT 'NOT_RELEASED',
    ADD COLUMN row_version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE narrative_investigations
    ADD CONSTRAINT ck_narrative_rollout_state
        CHECK (rollout_state IN ('NOT_RELEASED', 'ACTIVE', 'PAUSED'));

INSERT INTO narrative_investigation_versions (
    id, investigation_id, version, graph_json, review_status, approved_locales_json,
    reviewed_by, reviewed_at, revision, open_draft, created_at, updated_at)
SELECT gen_random_uuid(), ni.id, ni.version, ni.graph_json, ni.review_status,
       ni.approved_locales_json, ni.reviewed_by, ni.reviewed_at, 0,
       ni.review_status <> 'APPROVED', ni.created_at, ni.updated_at
FROM narrative_investigations ni;

UPDATE narrative_investigations ni
SET published_version_id = niv.id,
    rollout_state = CASE
        WHEN ni.enabled AND ni.review_status = 'APPROVED' THEN 'ACTIVE'
        WHEN NOT ni.enabled AND ni.review_status = 'APPROVED' THEN 'PAUSED'
        ELSE 'NOT_RELEASED'
    END
FROM narrative_investigation_versions niv
WHERE niv.investigation_id = ni.id
  AND niv.version = ni.version
  AND ni.review_status = 'APPROVED';

ALTER TABLE narrative_investigations
    ADD CONSTRAINT fk_narrative_published_version
        FOREIGN KEY (published_version_id) REFERENCES narrative_investigation_versions(id);

ALTER TABLE narrative_investigation_reviews
    ADD COLUMN version_id UUID,
    ADD COLUMN version INTEGER,
    ADD COLUMN revision BIGINT,
    ADD COLUMN audit_event_id UUID;

UPDATE narrative_investigation_reviews nir
SET version_id = niv.id,
    version = niv.version,
    revision = niv.revision
FROM narrative_investigation_versions niv
WHERE niv.investigation_id = nir.investigation_id;

ALTER TABLE narrative_investigation_reviews
    ADD CONSTRAINT fk_narrative_review_version
        FOREIGN KEY (version_id) REFERENCES narrative_investigation_versions(id),
    ADD CONSTRAINT fk_narrative_review_audit
        FOREIGN KEY (audit_event_id) REFERENCES admin_audit_events(id),
    ADD CONSTRAINT uq_narrative_review_audit UNIQUE (audit_event_id);

CREATE TABLE narrative_rollout_idempotency (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    idempotency_key VARCHAR(160) NOT NULL,
    action VARCHAR(32) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    response_json TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_narrative_rollout_idempotency UNIQUE (investigation_id, idempotency_key)
);

CREATE INDEX idx_narrative_versions_investigation
    ON narrative_investigation_versions(investigation_id, version DESC);
CREATE INDEX idx_narrative_rollout_idempotency_created
    ON narrative_rollout_idempotency(investigation_id, created_at DESC);
