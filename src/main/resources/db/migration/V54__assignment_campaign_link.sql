ALTER TABLE assignments ADD COLUMN IF NOT EXISTS campaign_id UUID REFERENCES quest_campaigns(id);
CREATE INDEX IF NOT EXISTS idx_assignments_campaign ON assignments(campaign_id);
