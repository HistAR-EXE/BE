CREATE TABLE IF NOT EXISTS quest_minigames (
    id          UUID PRIMARY KEY,
    engine      VARCHAR(32) NOT NULL,
    title       VARCHAR(255) NOT NULL,
    config      JSONB NOT NULL,
    CONSTRAINT quest_minigames_engine_chk CHECK (
        engine IN ('xep_tang', 'soi_nap', 'dao_gap', 'san_co_vat')
    )
);

CREATE TABLE IF NOT EXISTS user_game_results (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    game_id       UUID NOT NULL REFERENCES quest_minigames(id) ON DELETE CASCADE,
    best_stars    INT NOT NULL DEFAULT 0,
    attempts      INT NOT NULL DEFAULT 0,
    last_level_id VARCHAR(64),
    CONSTRAINT user_game_results_user_game_uq UNIQUE (user_id, game_id),
    CONSTRAINT user_game_results_stars_chk CHECK (best_stars BETWEEN 0 AND 3)
);

ALTER TABLE quest_steps
    ADD COLUMN IF NOT EXISTS minigame_id UUID REFERENCES quest_minigames(id);
