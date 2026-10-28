-- Preserve existing lead rows and legacy interest_site_code values while
-- accepting consultation requests for any proposed pilot site.
ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS organization_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS contact_role VARCHAR(160),
    ADD COLUMN IF NOT EXISTS site_location VARCHAR(255),
    ADD COLUMN IF NOT EXISTS available_materials TEXT,
    ADD COLUMN IF NOT EXISTS target_start VARCHAR(80);
