CREATE TABLE quest_campaigns (
    id UUID PRIMARY KEY,
    quest_id UUID NOT NULL,
    campaign_type VARCHAR(24) NOT NULL,
    title VARCHAR(160) NOT NULL,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_quest_campaigns_quest FOREIGN KEY (quest_id) REFERENCES quests(id),
    CONSTRAINT ck_quest_campaigns_window CHECK (ends_at > starts_at)
);
CREATE INDEX idx_quest_campaigns_active ON quest_campaigns (enabled, starts_at, ends_at);
