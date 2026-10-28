CREATE TABLE ai_quota_reservations (
 id uuid PRIMARY KEY,
 user_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE,
 organization_id uuid REFERENCES organizations(id) ON DELETE CASCADE,
 quota_date date NOT NULL,
 status varchar(12) NOT NULL CHECK (status IN ('ACTIVE','COMPLETED','RELEASED','EXPIRED')),
 expires_at timestamptz NOT NULL
);
CREATE INDEX idx_ai_quota_reservation_user ON ai_quota_reservations(user_id,quota_date,status,expires_at);
CREATE INDEX idx_ai_quota_reservation_org ON ai_quota_reservations(organization_id,quota_date,status,expires_at);
