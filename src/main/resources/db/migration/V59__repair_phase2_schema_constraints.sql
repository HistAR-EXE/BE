-- Reconcile databases that received Phase 2/3 tables from Hibernate before
-- Flyway V46-V57 was recorded. The earlier migrations are now idempotent;
-- this migration restores the constraints and indexes they require.

CREATE INDEX IF NOT EXISTS idx_journey_memories_user_expires ON journey_memories (user_id, expires_at);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_teacher_created ON teacher_ai_drafts (teacher_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_org_status ON teacher_ai_drafts (org_id, status);
CREATE INDEX IF NOT EXISTS idx_quest_campaigns_active ON quest_campaigns (enabled, starts_at, ends_at);
CREATE INDEX IF NOT EXISTS idx_narrative_investigation_reviews_investigation_time
    ON narrative_investigation_reviews (investigation_id, reviewed_at DESC);
CREATE INDEX IF NOT EXISTS idx_quest_campaign_audit_campaign_time
    ON quest_campaign_audit (campaign_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_assignments_campaign ON assignments(campaign_id);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_campaign ON teacher_ai_drafts(campaign_id);
CREATE INDEX IF NOT EXISTS idx_coop_lb_access_audit_time
    ON cooperative_leaderboard_access_audit(organization_id, occurred_at DESC);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_investigations_quest') THEN
        ALTER TABLE narrative_investigations ADD CONSTRAINT fk_narrative_investigations_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_progress_user') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT fk_narrative_progress_user FOREIGN KEY (user_id) REFERENCES profiles(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_progress_investigation') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT fk_narrative_progress_investigation FOREIGN KEY (investigation_id) REFERENCES narrative_investigations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_narrative_progress_user_investigation') THEN
        ALTER TABLE user_narrative_investigation_progress ADD CONSTRAINT uq_narrative_progress_user_investigation UNIQUE (user_id, investigation_id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_reward_ledger_user') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT fk_quest_reward_ledger_user FOREIGN KEY (user_id) REFERENCES profiles(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_reward_ledger_quest') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT fk_quest_reward_ledger_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_quest_reward_ledger_idempotency') THEN
        ALTER TABLE quest_reward_ledger ADD CONSTRAINT uq_quest_reward_ledger_idempotency UNIQUE (user_id, quest_id, reward_type, reward_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_campaigns_quest') THEN
        ALTER TABLE quest_campaigns ADD CONSTRAINT fk_quest_campaigns_quest FOREIGN KEY (quest_id) REFERENCES quests(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_quest_campaigns_window') THEN
        ALTER TABLE quest_campaigns ADD CONSTRAINT ck_quest_campaigns_window CHECK (ends_at > starts_at);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_narrative_investigation_reviews_investigation') THEN
        ALTER TABLE narrative_investigation_reviews ADD CONSTRAINT fk_narrative_investigation_reviews_investigation FOREIGN KEY (investigation_id) REFERENCES narrative_investigations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_quest_campaign_audit_campaign') THEN
        ALTER TABLE quest_campaign_audit ADD CONSTRAINT fk_quest_campaign_audit_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assignments_campaign') THEN
        ALTER TABLE assignments ADD CONSTRAINT fk_assignments_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_teacher_ai_drafts_campaign') THEN
        ALTER TABLE teacher_ai_drafts ADD CONSTRAINT fk_teacher_ai_drafts_campaign FOREIGN KEY (campaign_id) REFERENCES quest_campaigns(id);
    END IF;
END $$;
