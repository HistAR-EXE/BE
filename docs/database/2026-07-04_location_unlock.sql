-- Location progression: unlock after completing prerequisite quest (FR-15)
ALTER TABLE locations
    ADD COLUMN IF NOT EXISTS unlock_prerequisite_quest_id UUID REFERENCES quests(id),
    ADD COLUMN IF NOT EXISTS unlock_narrative TEXT;

-- Demo pair: Bến Nhà Rồng locked until Củ Chi quest complete
UPDATE locations
SET unlock_prerequisite_quest_id = '33333333-3333-3333-3333-333333333333',
    unlock_narrative = 'Manh mối từ Địa đạo Củ Chi dẫn bạn đến Bến Nhà Rồng — nơi Bác Hồ ra đi tìm đường cứu nước...'
WHERE id = '22222222-2222-2222-2222-222222222201';

UPDATE locations
SET unlock_prerequisite_quest_id = NULL,
    unlock_narrative = NULL
WHERE id = '11111111-1111-1111-1111-111111111111';
