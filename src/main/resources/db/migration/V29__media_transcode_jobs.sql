-- C2b: WebM (MediaRecorder) -> MP4 transcode jobs (FAILED_NO_FFMPEG when ffmpeg is unavailable).
CREATE TABLE IF NOT EXISTS media_transcode_jobs (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID REFERENCES profiles (id) ON DELETE SET NULL,
    status            VARCHAR(32)  NOT NULL DEFAULT 'PENDING',
    input_format      VARCHAR(16)  NOT NULL DEFAULT 'webm',
    output_format     VARCHAR(16)  NOT NULL DEFAULT 'mp4',
    input_url         TEXT,
    output_url        TEXT,
    input_size_bytes  BIGINT,
    error_message     TEXT,
    attempts          INT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    completed_at      TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_media_transcode_jobs_status_created
    ON media_transcode_jobs (status, created_at ASC);

CREATE INDEX IF NOT EXISTS idx_media_transcode_jobs_user
    ON media_transcode_jobs (user_id, created_at DESC);
