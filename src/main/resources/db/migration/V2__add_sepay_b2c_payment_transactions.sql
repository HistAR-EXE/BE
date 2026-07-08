CREATE TABLE IF NOT EXISTS b2c_payment_transactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES profiles(id),
    provider VARCHAR(30) NOT NULL,
    order_code VARCHAR(64) NOT NULL,
    transfer_content VARCHAR(64) NOT NULL,
    amount_vnd INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    return_to_path VARCHAR(255),
    qr_url TEXT,
    provider_transaction_id BIGINT,
    provider_reference_code VARCHAR(255),
    provider_gateway VARCHAR(100),
    provider_payload TEXT,
    expires_at TIMESTAMPTZ NOT NULL,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_order_code
    ON b2c_payment_transactions (order_code);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_transfer_content
    ON b2c_payment_transactions (transfer_content);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_payment_transactions_provider_tx
    ON b2c_payment_transactions (provider_transaction_id)
    WHERE provider_transaction_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_b2c_payment_transactions_user_created_at
    ON b2c_payment_transactions (user_id, created_at DESC);
