-- Immutable teacher reports captured from the server's live board.
CREATE TABLE org_report_snapshots (
    id UUID PRIMARY KEY,
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by UUID REFERENCES profiles(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    snapshot_json TEXT NOT NULL
);
CREATE INDEX idx_org_report_snapshots_recent
    ON org_report_snapshots (organization_id, created_at DESC);
