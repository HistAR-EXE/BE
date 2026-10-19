-- Nhiệm vụ v2 (Giai đoạn 1) — schema.
-- Spec: BE/docs/superpowers/specs/2026-10-19-quest-tracks-v2-design.md
--
-- quests: tách nhóm ONLINE / ONSITE, thứ tự hiển thị, độ khó, thời lượng.
-- quest_steps: bước bắt buộc / tùy chọn, bước Premium (luôn là tùy chọn).
-- Các cột cũ (step_discovery_keys, steps_total, completion_trigger, require_onsite_checkin,
-- required_order) được giữ một bản phát hành để tương thích; V84 đồng bộ chúng từ quest_steps.
-- Chạy lại an toàn (IF NOT EXISTS / kiểm tra pg_constraint).

ALTER TABLE quests ADD COLUMN IF NOT EXISTS track VARCHAR(16) NOT NULL DEFAULT 'ONLINE';
ALTER TABLE quests ADD COLUMN IF NOT EXISTS sort_order INT NOT NULL DEFAULT 100;
ALTER TABLE quests ADD COLUMN IF NOT EXISTS difficulty VARCHAR(16);
ALTER TABLE quests ADD COLUMN IF NOT EXISTS estimated_minutes INT;

ALTER TABLE quest_steps ADD COLUMN IF NOT EXISTS is_required BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE quest_steps ADD COLUMN IF NOT EXISTS is_premium BOOLEAN NOT NULL DEFAULT FALSE;

-- Thứ tự cũ (required_order 1, 2, 3…) → sort_order 10, 20, 30…; nhiệm vụ không có thứ tự giữ 100.
UPDATE quests
SET sort_order = required_order * 10
WHERE required_order IS NOT NULL
  AND required_order > 0
  AND sort_order = 100;

-- Bước cổng thời gian cần Premium đang có trong dữ liệu → tùy chọn (quyết định #2).
UPDATE quest_steps
SET is_premium = TRUE,
    is_required = FALSE
WHERE action_type = 'portal'
  AND portal_era IN (1948, 1968)
  AND (is_premium = FALSE OR is_required = TRUE);

-- Chuẩn hoá step_order trước khi thêm UNIQUE: chỉ những nhiệm vụ đang trùng hoặc thiếu thứ tự.
WITH broken AS (
    SELECT quest_id
    FROM quest_steps
    GROUP BY quest_id
    HAVING COUNT(*) <> COUNT(DISTINCT step_order)
),
ranked AS (
    SELECT s.id,
           ROW_NUMBER() OVER (PARTITION BY s.quest_id ORDER BY s.step_order NULLS LAST, s.id) AS rn
    FROM quest_steps s
    JOIN broken b ON b.quest_id = s.quest_id
)
UPDATE quest_steps s
SET step_order = r.rn
FROM ranked r
WHERE s.id = r.id
  AND s.step_order IS DISTINCT FROM r.rn;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quests_track') THEN
        ALTER TABLE quests
            ADD CONSTRAINT chk_quests_track CHECK (track IN ('ONLINE', 'ONSITE'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quests_difficulty') THEN
        ALTER TABLE quests
            ADD CONSTRAINT chk_quests_difficulty CHECK (difficulty IS NULL OR difficulty IN ('EASY', 'MEDIUM', 'HARD'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_quest_steps_premium_optional') THEN
        ALTER TABLE quest_steps
            ADD CONSTRAINT chk_quest_steps_premium_optional CHECK (NOT (is_premium AND is_required));
    END IF;
    -- DEFERRABLE: V84 và trình sửa bước sau này đổi thứ tự nhiều bước trong cùng một giao dịch.
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uq_quest_steps_quest_order') THEN
        ALTER TABLE quest_steps
            ADD CONSTRAINT uq_quest_steps_quest_order UNIQUE (quest_id, step_order) DEFERRABLE INITIALLY DEFERRED;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_quests_location_track_sort ON quests (location_id, track, sort_order);
