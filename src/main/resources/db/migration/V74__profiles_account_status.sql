-- Admin IAM P0: suspend / unsuspend accounts
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS account_status VARCHAR(20);

UPDATE profiles
SET account_status = 'ACTIVE'
WHERE account_status IS NULL OR BTRIM(account_status) = '';

ALTER TABLE profiles
    ALTER COLUMN account_status SET DEFAULT 'ACTIVE';

ALTER TABLE profiles
    ALTER COLUMN account_status SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_profiles_account_status'
    ) THEN
        ALTER TABLE profiles
            ADD CONSTRAINT chk_profiles_account_status
            CHECK (UPPER(account_status) IN ('ACTIVE', 'SUSPENDED'));
    END IF;
END $$;
