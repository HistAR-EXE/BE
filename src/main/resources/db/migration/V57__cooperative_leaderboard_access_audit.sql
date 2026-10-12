CREATE TABLE cooperative_leaderboard_access_audit (
    id UUID PRIMARY KEY,
    actor_id UUID,
    organization_id UUID NOT NULL,
    campaign_id UUID NOT NULL,
    allowed BOOLEAN NOT NULL,
    outcome VARCHAR(80) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    detail TEXT
);
CREATE INDEX idx_coop_lb_access_audit_time ON cooperative_leaderboard_access_audit(organization_id, occurred_at DESC);
