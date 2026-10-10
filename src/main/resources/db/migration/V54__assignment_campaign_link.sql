ALTER TABLE assignments ADD COLUMN campaign_id UUID REFERENCES quest_campaigns(id);
CREATE INDEX idx_assignments_campaign ON assignments(campaign_id);
