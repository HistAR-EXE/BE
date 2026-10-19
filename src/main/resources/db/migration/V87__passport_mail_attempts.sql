-- Reconcile both a new database and a table created earlier by Hibernate ddl-auto=update.
-- Fail on incompatible/orphaned existing data; never silently remove user history.
CREATE TABLE IF NOT EXISTS passport_mail_attempts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    request_id UUID NOT NULL,
    image_sha256 VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    state VARCHAR(16) NOT NULL
);

ALTER TABLE passport_mail_attempts
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN request_id SET NOT NULL,
    ALTER COLUMN image_sha256 SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN state SET NOT NULL;

DO $passport_mail$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'fk_passport_mail_profile'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT fk_passport_mail_profile
            FOREIGN KEY (user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'ck_passport_mail_image_hash'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT ck_passport_mail_image_hash
            CHECK (image_sha256 ~ '^[0-9a-f]{64}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND conname = 'ck_passport_mail_state'
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT ck_passport_mail_state
            CHECK (state IN ('CREATED','ACCEPTED','UNKNOWN'));
    END IF;

    -- Hibernate may already have generated the same unique constraint under a different name.
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'passport_mail_attempts'::regclass
          AND contype IN ('u','p')
          AND conkey = ARRAY[
              (SELECT attnum FROM pg_attribute WHERE attrelid = 'passport_mail_attempts'::regclass AND attname = 'user_id'),
              (SELECT attnum FROM pg_attribute WHERE attrelid = 'passport_mail_attempts'::regclass AND attname = 'request_id')
          ]::smallint[]
    ) THEN
        ALTER TABLE passport_mail_attempts
            ADD CONSTRAINT uq_passport_mail_user_request UNIQUE (user_id, request_id);
    END IF;
END;
$passport_mail$;

CREATE INDEX IF NOT EXISTS idx_passport_mail_user_created
    ON passport_mail_attempts(user_id, created_at DESC);

