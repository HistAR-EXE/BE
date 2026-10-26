-- V72 is a historical analytics seed that references quests 334 and 335.
-- Older databases received those quests through a manual seed, while a clean
-- database did not get them until V84. Create the approved content early so a
-- clean Flyway run remains valid without changing V72's production checksum.

INSERT INTO quests (
    id, location_id, title, description, story, points_reward, cover_image,
    completion_trigger, steps_total, step_discovery_keys
)
SELECT
    v.id::uuid,
    '11111111-1111-1111-1111-111111111111'::uuid,
    v.title,
    v.description,
    v.story,
    v.points,
    '/media/heritage/dia-dao-cu-chi/cover.jpeg',
    'discovery',
    3,
    NULL
FROM (VALUES
    (
        '33333333-3333-3333-3333-333333333334',
        'Mật mã Sa bàn Tác chiến',
        'Tìm hiểu cấu trúc 3 tầng ngầm và nghệ thuật du kích bẫy chông qua sa bàn quân sự.',
        'Tại Củ Chi, vũ khí thô sơ đã chiến thắng khí tài hiện đại. Hệ thống 3 tầng ngầm với bẫy chông kẹp nách và chiến thuật đánh du kích đã biến Củ Chi thành "Đất Thép thành đồng".',
        150
    ),
    (
        '33333333-3333-3333-3333-333333333335',
        'Ánh sáng Đèn Mù U',
        'Giải mã cuộc sống sinh hoạt, y tế và giáo dục dưới lòng đất của quân dân Củ Chi.',
        'Dưới những lớp đất tối tăm, cuộc sống vẫn nảy mầm. Ánh sáng le lói từ chiếc đèn dầu mù u đã soi sáng những lớp học bình dân và trạm xá dã chiến.',
        120
    )
) AS v(id, title, description, story, points)
WHERE EXISTS (
    SELECT 1 FROM locations
    WHERE id = '11111111-1111-1111-1111-111111111111'::uuid
)
ON CONFLICT (id) DO NOTHING;
