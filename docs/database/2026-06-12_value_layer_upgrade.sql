-- HistAR Value Layer upgrade (visit_sessions, quest steps, bindings, composite artifacts, analytics events, B2B2E stub)

-- Quest multi-step: comma-separated discovery keys before check-in complete
ALTER TABLE quests ADD COLUMN IF NOT EXISTS step_discovery_keys TEXT;

UPDATE quests
SET step_discovery_keys = 'scene:22222222-2222-2222-2222-222222222221,scene:22222222-2222-2222-2222-222222222223,era:1948'
WHERE location_id = '11111111-1111-1111-1111-111111111111'
  AND step_discovery_keys IS NULL;

-- Visit sessions (journey / B2B analytics foundation)
CREATE TABLE IF NOT EXISTS visit_sessions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES profiles(id),
    location_id     UUID NOT NULL REFERENCES locations(id),
    mode            VARCHAR(16) NOT NULL DEFAULT 'online',
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    ended_at        TIMESTAMPTZ,
    checkin_id      UUID REFERENCES checkins(id)
);
CREATE INDEX IF NOT EXISTS idx_visit_sessions_location_started ON visit_sessions(location_id, started_at);
CREATE INDEX IF NOT EXISTS idx_visit_sessions_user ON visit_sessions(user_id, started_at DESC);

CREATE TABLE IF NOT EXISTS visit_session_events (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_session_id   UUID NOT NULL REFERENCES visit_sessions(id) ON DELETE CASCADE,
    event_type         VARCHAR(32) NOT NULL,
    event_key          VARCHAR(128),
    source             VARCHAR(32),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_visit_session_events_session ON visit_session_events(visit_session_id, created_at);

-- Multi-site discovery bindings (replaces FE hardcode over time)
CREATE TABLE IF NOT EXISTS discovery_content_bindings (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id     UUID NOT NULL REFERENCES locations(id),
    unlock_key        VARCHAR(128) NOT NULL,
    record_key        VARCHAR(128) NOT NULL,
    engagement      VARCHAR(32) NOT NULL,
    href_template     TEXT NOT NULL,
    sort_order        INT DEFAULT 0,
    UNIQUE (location_id, unlock_key)
);

-- Composite artifact unlock requirements
CREATE TABLE IF NOT EXISTS artifact_unlock_requirements (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id             UUID NOT NULL REFERENCES locations(id),
    artifact_unlock_key       VARCHAR(128) NOT NULL,
    required_discovery_key  VARCHAR(128) NOT NULL,
    UNIQUE (location_id, artifact_unlock_key, required_discovery_key)
);

-- 3-layer analytics events (impression / engagement / discovery)
CREATE TABLE IF NOT EXISTS analytics_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID REFERENCES profiles(id),
    location_id     UUID REFERENCES locations(id),
    visit_session_id UUID REFERENCES visit_sessions(id),
    event_type      VARCHAR(32) NOT NULL,
    event_key       VARCHAR(128),
    source          VARCHAR(32),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_analytics_events_location ON analytics_events(location_id, event_type, created_at);

-- B2B2E stub (organizations)
CREATE TABLE IF NOT EXISTS organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug        VARCHAR(64) UNIQUE NOT NULL,
    name        VARCHAR(255) NOT NULL,
    plan        VARCHAR(32) NOT NULL DEFAULT 'trial',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS organization_members (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID NOT NULL REFERENCES organizations(id),
    user_id          UUID NOT NULL REFERENCES profiles(id),
    org_role         VARCHAR(16) NOT NULL,
    invited_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at      TIMESTAMPTZ,
    UNIQUE (organization_id, user_id)
);

INSERT INTO organizations (id, slug, name, plan)
VALUES ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'cu-chi-demo', 'Địa đạo Củ Chi (Demo)', 'trial')
ON CONFLICT (slug) DO NOTHING;
