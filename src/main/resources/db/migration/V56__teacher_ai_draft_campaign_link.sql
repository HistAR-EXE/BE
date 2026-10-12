ALTER TABLE teacher_ai_drafts ADD COLUMN campaign_id UUID REFERENCES quest_campaigns(id);
CREATE INDEX idx_teacher_ai_drafts_campaign ON teacher_ai_drafts(campaign_id);
