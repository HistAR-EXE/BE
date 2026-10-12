CREATE TABLE IF NOT EXISTS teacher_ai_drafts (
    id UUID PRIMARY KEY,
    org_id UUID NOT NULL,
    teacher_id UUID NOT NULL,
    quest_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_version VARCHAR(255) NOT NULL,
    answer_blocks_json TEXT NOT NULL,
    approved_locales_json TEXT NOT NULL,
    evaluation_passed BOOLEAN NOT NULL,
    evaluation_reasons TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    due_at TIMESTAMP WITH TIME ZONE,
    published_assignment_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_teacher_created ON teacher_ai_drafts (teacher_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_teacher_ai_drafts_org_status ON teacher_ai_drafts (org_id, status);
