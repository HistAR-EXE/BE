-- B2B2C: which pilot site the lead is interested in (cu-chi | hoang-thanh-thang-long | dai-noi-hue)
ALTER TABLE heritage_digitization_inquiries
    ADD COLUMN IF NOT EXISTS interest_site_code VARCHAR(64) NULL;

CREATE INDEX IF NOT EXISTS idx_heritage_inq_interest_site
    ON heritage_digitization_inquiries (interest_site_code);
