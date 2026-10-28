-- V103 retired both duplicate optional portal steps. Restore one 1968 step
-- for the tactical Sa ban quest and use the distinct 1975 chapter for Den Mu U.
-- Keep existing user_discoveries and completed quest progress untouched.

-- The 1975 chapter can be viewed, but without this point its completion
-- callback cannot record era:1975 on the server.
INSERT INTO discovery_points (id, location_id, name, map_x_pct, map_y_pct, unlock_key, sort_order)
SELECT 'd0000001-0000-4000-8000-000000000013'::uuid,
       '11111111-1111-1111-1111-111111111111'::uuid,
       'Ngày giải phóng 1975', 50.00, 28.00, 'era:1975', 13
WHERE EXISTS (SELECT 1 FROM locations WHERE id = '11111111-1111-1111-1111-111111111111'::uuid)
ON CONFLICT DO NOTHING;

INSERT INTO quest_steps (id, quest_id, step_order, unlock_key, title, objective,
                         description, hint, action_type, action_label, portal_era,
                         preview_image, is_required, is_premium)
SELECT v.id::uuid, v.quest_id::uuid, 3, v.unlock_key, v.title, v.objective,
       v.description, v.hint, 'portal', 'Mở Cổng thời gian', v.portal_era,
       '/media/heritage/dia-dao-cu-chi/cover.jpeg', FALSE, TRUE
FROM (VALUES
    ('33333333-3333-3333-3333-333333333343',
     '33333333-3333-3333-3333-333333333334',
     'era:1968', 'Cổng thời gian · 1968',
     'Xem hết chương 1968 trong Dòng thời gian (tới câu hỏi cuối chương).',
     'Bước mở rộng Premium về bối cảnh tác chiến năm 1968; không bắt buộc để hoàn thành nhiệm vụ.',
     'Cổng thời gian → Dòng thời gian → 1968.', 1968),
    ('33333333-3333-3333-3333-333333333353',
     '33333333-3333-3333-3333-333333333335',
     'era:1975', 'Cổng thời gian · 1975',
     'Xem hết chương 1975 trong Dòng thời gian (tới câu hỏi cuối chương).',
     'Bước mở rộng Premium về những năm cuối của đời sống trong lòng đất; không bắt buộc để hoàn thành nhiệm vụ.',
     'Cổng thời gian → Dòng thời gian → 1975.', 1975)
) AS v(id, quest_id, unlock_key, title, objective, description, hint, portal_era)
WHERE EXISTS (SELECT 1 FROM quests q WHERE q.id = v.quest_id::uuid)
ON CONFLICT (quest_id, unlock_key) DO UPDATE
SET step_order = EXCLUDED.step_order,
    title = EXCLUDED.title,
    objective = EXCLUDED.objective,
    description = EXCLUDED.description,
    hint = EXCLUDED.hint,
    action_type = EXCLUDED.action_type,
    action_label = EXCLUDED.action_label,
    portal_era = EXCLUDED.portal_era,
    is_required = EXCLUDED.is_required,
    is_premium = EXCLUDED.is_premium;

WITH agg AS (
    SELECT s.quest_id, COUNT(*) AS total,
           STRING_AGG(s.unlock_key, ',' ORDER BY s.step_order)
               FILTER (WHERE s.is_required) AS required_keys
    FROM quest_steps s
    WHERE s.quest_id IN (
        '33333333-3333-3333-3333-333333333334'::uuid,
        '33333333-3333-3333-3333-333333333335'::uuid
    )
    GROUP BY s.quest_id
)
UPDATE quests q
SET steps_total = agg.total,
    step_discovery_keys = agg.required_keys
FROM agg
WHERE q.id = agg.quest_id;
