-- The optional 1968 portal action was shown twice across the Sa ban and
-- Den Mu U quests. It is not part of either quest's required completion.
-- Preserve user_discoveries and completed quest progress; only retire the
-- duplicate step definitions and refresh the legacy counters.
UPDATE discovery_content_bindings
SET quest_step_id = NULL
WHERE quest_step_id IN (
    '33333333-3333-3333-3333-333333333343'::uuid,
    '33333333-3333-3333-3333-333333333353'::uuid
);

DELETE FROM quest_steps
WHERE id IN (
    '33333333-3333-3333-3333-333333333343'::uuid,
    '33333333-3333-3333-3333-333333333353'::uuid
)
AND unlock_key = 'era:1968';

WITH agg AS (
    SELECT s.quest_id,
           COUNT(*) AS total,
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
