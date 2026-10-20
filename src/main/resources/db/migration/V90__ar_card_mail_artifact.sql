-- Existing NULL values represent historical bulk-card emails; never assign them to an artifact.
ALTER TABLE ar_card_mail_attempts ADD COLUMN IF NOT EXISTS artifact_id VARCHAR(32);
