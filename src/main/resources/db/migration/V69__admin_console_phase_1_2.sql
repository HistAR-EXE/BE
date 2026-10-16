CREATE TABLE content_report_cases (
    event_id UUID PRIMARY KEY REFERENCES pilot_events(id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL DEFAULT 'NEW',
    admin_note TEXT,
    reviewed_by UUID REFERENCES profiles(id),
    reviewed_at TIMESTAMPTZ,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_content_report_case_status
        CHECK (status IN ('NEW', 'IN_REVIEW', 'RESOLVED', 'REJECTED'))
);

INSERT INTO content_report_cases (event_id, status, created_at, updated_at)
SELECT id, 'NEW', COALESCE(created_at, NOW()), COALESCE(created_at, NOW())
FROM pilot_events
WHERE event_type = 'content_report'
ON CONFLICT (event_id) DO NOTHING;

CREATE INDEX idx_content_report_cases_status_updated
    ON content_report_cases (status, updated_at DESC);

ALTER TABLE org_payment_transactions
    ADD COLUMN license_count INTEGER,
    ADD COLUMN gross_amount_vnd BIGINT,
    ADD COLUMN discount_amount_vnd BIGINT,
    ADD COLUMN subscription_id UUID;

ALTER TABLE org_payment_transactions
    ADD CONSTRAINT ck_org_payment_license_count CHECK (license_count IS NULL OR license_count > 0),
    ADD CONSTRAINT ck_org_payment_gross_amount CHECK (gross_amount_vnd IS NULL OR gross_amount_vnd >= 0),
    ADD CONSTRAINT ck_org_payment_discount_amount CHECK (discount_amount_vnd IS NULL OR discount_amount_vnd >= 0),
    ADD CONSTRAINT fk_org_payment_subscription FOREIGN KEY (subscription_id) REFERENCES subscriptions(id);

CREATE INDEX idx_org_payment_transactions_subscription
    ON org_payment_transactions (subscription_id) WHERE subscription_id IS NOT NULL;

ALTER TABLE subscriptions
    ADD COLUMN source_payment_transaction_id UUID,
    ADD COLUMN billing_cadence VARCHAR(24),
    ADD COLUMN contract_value_vnd BIGINT,
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN quantity INTEGER;

ALTER TABLE subscriptions
    ADD CONSTRAINT fk_subscription_source_payment
        FOREIGN KEY (source_payment_transaction_id) REFERENCES org_payment_transactions(id),
    ADD CONSTRAINT ck_subscription_billing_cadence
        CHECK (billing_cadence IS NULL OR billing_cadence IN ('MONTHLY', 'QUARTERLY', 'ANNUAL', 'ONE_TIME')),
    ADD CONSTRAINT ck_subscription_contract_value
        CHECK (contract_value_vnd IS NULL OR contract_value_vnd >= 0),
    ADD CONSTRAINT ck_subscription_currency
        CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$'),
    ADD CONSTRAINT ck_subscription_quantity CHECK (quantity IS NULL OR quantity > 0);

CREATE UNIQUE INDEX uq_subscriptions_source_payment
    ON subscriptions (source_payment_transaction_id)
    WHERE source_payment_transaction_id IS NOT NULL;
