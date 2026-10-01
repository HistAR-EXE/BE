-- QuestStep entity — see docs/sql/seed/2026-07-08_quest_steps_schema.sql

CREATE TABLE IF NOT EXISTS quest_steps (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    quest_id      UUID NOT NULL REFERENCES quests(id) ON DELETE CASCADE,
    step_order    INT,
    unlock_key    VARCHAR(255) NOT NULL,
    title         VARCHAR(255),
    objective     VARCHAR(500),
    description   TEXT,
    hint          TEXT,
    action_type   VARCHAR(50),
    action_label  VARCHAR(100),
    xp_partial    INT,
    chat_prompt   TEXT,
    portal_era    INT,
    preview_image TEXT
);

CREATE INDEX IF NOT EXISTS idx_quest_steps_quest_order
    ON quest_steps (quest_id, step_order);

CREATE UNIQUE INDEX IF NOT EXISTS uq_quest_steps_quest_unlock
    ON quest_steps (quest_id, unlock_key);
