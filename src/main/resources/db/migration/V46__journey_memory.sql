CREATE TABLE journey_memories (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_visited_poi VARCHAR(255),
    journey_summary VARCHAR(1000),
    locale VARCHAR(16),
    preferred_mode VARCHAR(32),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_journey_memories_user_location UNIQUE (user_id, location_id)
);

CREATE INDEX idx_journey_memories_user_expires ON journey_memories (user_id, expires_at);
