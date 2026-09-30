ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS admin_notes TEXT;

ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS contacted_at TIMESTAMPTZ NULL;
