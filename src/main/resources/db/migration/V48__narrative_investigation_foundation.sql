CREATE TABLE IF NOT EXISTS narrative_investigations (
    id UUID PRIMARY KEY,
    quest_id UUID NOT NULL UNIQUE REFERENCES quests(id),
    version INTEGER NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    graph_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS user_narrative_investigation_progress (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id),
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    state_json TEXT NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_narrative_progress_user_investigation UNIQUE (user_id, investigation_id)
);

CREATE TABLE IF NOT EXISTS quest_reward_ledger (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES profiles(id),
    quest_id UUID NOT NULL REFERENCES quests(id),
    reward_type VARCHAR(48) NOT NULL,
    reward_key VARCHAR(255) NOT NULL,
    xp INTEGER NOT NULL,
    awarded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_quest_reward_ledger_idempotency UNIQUE (user_id, quest_id, reward_type, reward_key)
);
