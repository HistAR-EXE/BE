-- Tables that historically came from Hibernate ddl-auto before Flyway ran.
-- Production databases already have these tables. A brand-new database needs
-- them before V13, V44 and V72 can extend/seed them.

CREATE TABLE IF NOT EXISTS visit_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    mode VARCHAR(64),
    status VARCHAR(64),
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    last_activity_at TIMESTAMPTZ,
    ended_reason VARCHAR(255),
    checkin_id UUID REFERENCES checkins(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS visit_session_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    visit_session_id UUID NOT NULL REFERENCES visit_sessions(id) ON DELETE CASCADE,
    event_type VARCHAR(100),
    event_key VARCHAR(255),
    source VARCHAR(100),
    poi_name_snapshot VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS analytics_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID REFERENCES profiles(id) ON DELETE CASCADE,
    location_id UUID REFERENCES locations(id) ON DELETE SET NULL,
    visit_session_id UUID REFERENCES visit_sessions(id) ON DELETE CASCADE,
    event_type VARCHAR(100),
    event_key VARCHAR(255),
    source VARCHAR(100),
    content_type VARCHAR(100),
    metadata TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS artifacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_id UUID REFERENCES locations(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    image_url TEXT,
    description TEXT,
    story TEXT,
    unlock_key VARCHAR(255),
    reliability VARCHAR(64),
    sort_order INTEGER DEFAULT 0
);

CREATE TABLE IF NOT EXISTS user_discoveries (
    user_id UUID NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
    location_id UUID NOT NULL REFERENCES locations(id) ON DELETE CASCADE,
    discovery_key VARCHAR(255) NOT NULL,
    discovered_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, location_id, discovery_key)
);

CREATE INDEX IF NOT EXISTS idx_visit_sessions_user_id
    ON visit_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_visit_session_events_session_id
    ON visit_session_events(visit_session_id);
CREATE INDEX IF NOT EXISTS idx_analytics_events_user_created_at
    ON analytics_events(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_artifacts_location_id
    ON artifacts(location_id);
