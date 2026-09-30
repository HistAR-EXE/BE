CREATE TABLE pilot_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_uuid UUID UNIQUE,
    user_id UUID,
    session_id UUID,
    event_type VARCHAR(64) NOT NULL,
    station_code VARCHAR(32),
    payload_json TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_pilot_events_event_type ON pilot_events (event_type);
CREATE INDEX idx_pilot_events_occurred_at ON pilot_events (occurred_at);
CREATE INDEX idx_pilot_events_user_id ON pilot_events (user_id);
