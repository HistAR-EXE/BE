-- Add B2C renewal reminder persistence
-- Needed because Hibernate ddl-auto=validate requires table existence.

CREATE TABLE IF NOT EXISTS b2c_renewal_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID NOT NULL REFERENCES b2c_subscriptions(id),
    reminder_day INT NOT NULL,
    sent_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_b2c_renewal_reminders_subscription_day
    ON b2c_renewal_reminders (subscription_id, reminder_day);

