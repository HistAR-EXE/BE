-- B7 Tiểu đội co-op (squads)

CREATE TABLE squads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(6) NOT NULL,
    site_code VARCHAR(64),
    leader_user_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_squads_code UNIQUE (code)
);

CREATE INDEX idx_squads_leader_user_id ON squads (leader_user_id);
CREATE INDEX idx_squads_site_code ON squads (site_code);

CREATE TABLE squad_members (
    squad_id UUID NOT NULL REFERENCES squads (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (squad_id, user_id)
);

CREATE INDEX idx_squad_members_user_id ON squad_members (user_id);
