ALTER TABLE quest_campaigns ADD COLUMN pilot_status VARCHAR(24) NOT NULL DEFAULT 'PENDING';
ALTER TABLE quest_campaigns ADD COLUMN release_decision VARCHAR(24) NOT NULL DEFAULT 'PENDING';
ALTER TABLE quest_campaigns ADD COLUMN release_note TEXT;
ALTER TABLE quest_campaigns ADD COLUMN release_decided_by UUID;
ALTER TABLE quest_campaigns ADD COLUMN release_decided_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE quest_campaign_audit ADD COLUMN rollback_reason TEXT;
