CREATE TABLE quest_campaign_audit (
  id UUID PRIMARY KEY, campaign_id UUID NOT NULL REFERENCES quest_campaigns(id), action VARCHAR(32) NOT NULL,
  actor_id UUID NOT NULL, occurred_at TIMESTAMP WITH TIME ZONE NOT NULL, detail_json TEXT NOT NULL
);
CREATE INDEX idx_quest_campaign_audit_campaign_time ON quest_campaign_audit(campaign_id, occurred_at DESC);
