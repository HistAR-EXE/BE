CREATE TABLE IF NOT EXISTS narrative_investigation_reviews (
    id UUID PRIMARY KEY,
    investigation_id UUID NOT NULL REFERENCES narrative_investigations(id),
    decision VARCHAR(24) NOT NULL,
    approved_locales_json TEXT NOT NULL,
    review_note TEXT,
    reviewed_by VARCHAR(160) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_narrative_investigation_reviews_investigation_time
    ON narrative_investigation_reviews (investigation_id, reviewed_at DESC);
