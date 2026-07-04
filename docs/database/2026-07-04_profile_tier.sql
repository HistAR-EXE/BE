-- Freemium tier on profiles (FREE / PREMIUM)
ALTER TABLE profiles ADD COLUMN IF NOT EXISTS tier VARCHAR(20) NOT NULL DEFAULT 'FREE';

UPDATE profiles SET tier = 'FREE' WHERE tier IS NULL OR tier = '';
