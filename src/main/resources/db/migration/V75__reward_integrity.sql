-- Historical totals are retained as an explicitly unverified opening balance.
ALTER TABLE messages ADD COLUMN IF NOT EXISTS server_generated BOOLEAN NOT NULL DEFAULT FALSE;

-- Viewing the Cedar Falls diorama is distinct from finding the 360 tour's sa-ban fragment.
INSERT INTO discovery_points(id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
VALUES ('d0000001-0000-4000-8000-000000000123', '11111111-1111-1111-1111-111111111111',
    'Trải nghiệm sa bàn Cedar Falls (không thưởng XP)', 50, 50, 'feature:diorama', 0)
ON CONFLICT(location_id, unlock_key) DO NOTHING;

-- Existing local databases may already contain Hibernate-created tables. Adopt them
-- without dropping records; defaults and constraints are installed below as well.
CREATE TABLE IF NOT EXISTS xp_ledger (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    event_key VARCHAR(255) NOT NULL,
    points INTEGER NOT NULL,
    awarded_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, event_key)
);
ALTER TABLE xp_ledger ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE xp_ledger ALTER COLUMN awarded_at SET DEFAULT now();

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass
        AND contype = 'u' AND pg_get_constraintdef(oid) = 'UNIQUE (user_id, event_key)') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_user_event_unique UNIQUE(user_id, event_key);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass AND conname = 'xp_ledger_points_nonnegative') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_points_nonnegative CHECK(points >= 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'xp_ledger'::regclass AND conname = 'xp_ledger_user_fk') THEN
        ALTER TABLE xp_ledger ADD CONSTRAINT xp_ledger_user_fk FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
    -- A missing opening balance may be reconstructed only if journal entries
    -- do not already exceed the account total. Never silently rewrite a total.
    IF EXISTS (SELECT 1 FROM profiles p WHERE NOT EXISTS (
        SELECT 1 FROM xp_ledger x WHERE x.user_id = p.id AND x.event_key = 'baseline:v1')
        AND COALESCE((SELECT SUM(x.points) FROM xp_ledger x WHERE x.user_id = p.id), 0)
            > GREATEST(COALESCE(p.total_points, 0), 0)) THEN
        RAISE EXCEPTION 'XP journal exceeds account total; reconcile historical data before V69.';
    END IF;
END $$;

INSERT INTO xp_ledger(id, user_id, event_key, points, awarded_at)
SELECT gen_random_uuid(), p.id, 'baseline:v1',
    GREATEST(COALESCE(p.total_points, 0), 0) -
        COALESCE((SELECT SUM(x.points) FROM xp_ledger x WHERE x.user_id = p.id), 0), now()
FROM profiles p
WHERE NOT EXISTS (SELECT 1 FROM xp_ledger x WHERE x.user_id = p.id AND x.event_key = 'baseline:v1')
ON CONFLICT(user_id, event_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS explore_attempts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    location_id UUID NOT NULL,
    challenge_key VARCHAR(128) NOT NULL,
    answer INTEGER,
    passed BOOLEAN NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE explore_attempts ALTER COLUMN id SET DEFAULT gen_random_uuid();
ALTER TABLE explore_attempts ALTER COLUMN submitted_at SET DEFAULT now();
DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_answer_range') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_answer_range CHECK(answer BETWEEN 0 AND 3);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_user_fk') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_user_fk FOREIGN KEY(user_id) REFERENCES profiles(id) ON DELETE CASCADE;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conrelid = 'explore_attempts'::regclass AND conname = 'explore_attempt_location_fk') THEN
        ALTER TABLE explore_attempts ADD CONSTRAINT explore_attempt_location_fk FOREIGN KEY(location_id) REFERENCES locations(id);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_indexes WHERE schemaname = current_schema()
        AND tablename = 'explore_attempts' AND indexdef LIKE '% USING btree (user_id, submitted_at)') THEN
        CREATE INDEX idx_explore_attempts_user_time ON explore_attempts(user_id, submitted_at);
    END IF;
END $$;

INSERT INTO badges(id, name, description, icon_url, condition_type, condition_value, location_id) VALUES
('b0000001-0000-4000-8000-000000000201', 'La Bàn Khởi Nguyên', '50 XP toàn tài khoản hoặc hoàn thành một thử thách tầng.', '/badges/badge-2.png', 'explore', 1, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000202', 'Dấu Chân Đất Thép', 'Hoàn thành bốn thử thách tầng.', '/badges/badge-5.png', 'explore', 2, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000203', 'Bậc Thầy Cơ Yếu', 'Giải ba mật thư.', '/badges/badge-8.png', 'explore', 3, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000204', 'Thám Tử Sử Liệu', 'Bốn chứng cứ và kết luận được server xác nhận.', '/badges/badge-6.png', 'explore', 4, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000205', 'Tri Kỷ Tiền Nhân', 'Có hội thoại AI, 300 XP toàn tài khoản và hai hồ sơ.', '/badges/badge-1.png', 'explore', 5, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000206', 'Người Giữ Kho Báu', 'Mở khóa cổ vật, 500 XP toàn tài khoản và ba mật thư.', '/badges/badge-7.png', 'explore', 6, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000207', 'Tinh Hoa Bác Học', 'Hoàn thành sáu hồ sơ.', '/badges/badge-3.png', 'explore', 7, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000208', 'Sứ Giả Thời Không', 'Có trải nghiệm 360°, sa bàn và 600 XP toàn tài khoản.', '/badges/badge-4.png', 'explore', 8, '11111111-1111-1111-1111-111111111111'),
('b0000001-0000-4000-8000-000000000209', 'Nhãn Quan Xuyên Thấu', '1.130 XP toàn tài khoản, 360°, sa bàn, AI và mở khóa cổ vật.', '/badges/badge-10.png', 'explore', 9, '11111111-1111-1111-1111-111111111111')
ON CONFLICT (id) DO NOTHING;
