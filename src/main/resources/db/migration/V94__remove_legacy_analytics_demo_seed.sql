-- Remove only the deterministic 120-account analytics seed introduced by V72.
-- Heritage content is deliberately untouched. The older named platform/demo
-- accounts are handled by the reviewed cleanup tool after a real admin exists,
-- so ownership can be transferred safely instead of guessed during migration.

CREATE TEMP TABLE legacy_analytics_seed_users ON COMMIT DROP AS
SELECT id
FROM profiles
WHERE email LIKE 'seed-analytics-%@histar.vn';

-- The June seed creates visit_sessions.user_id without ON DELETE CASCADE.
-- V0_5 cannot replace that constraint, so these rows must be removed first.
DELETE FROM visit_session_events
WHERE visit_session_id IN (
    SELECT vs.id
    FROM visit_sessions vs
    WHERE vs.user_id IN (SELECT id FROM legacy_analytics_seed_users)
);

DELETE FROM analytics_events
WHERE metadata = 'seed-admin-analytics-cu-chi'
   OR user_id IN (SELECT id FROM legacy_analytics_seed_users);

DELETE FROM visit_sessions
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users);

DELETE FROM pilot_events
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users)
   OR payload_json LIKE '%"seed":"admin-analytics-cu-chi"%';

DELETE FROM checkins
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users);

DELETE FROM user_discoveries
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users);

DELETE FROM user_quest_progress
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users);

-- Rows added after V72 are removed explicitly when their FK does not cascade.
SELECT demo_delete_by_ids(
    'quest_reward_ledger',
    'user_id',
    ARRAY(SELECT id FROM legacy_analytics_seed_users)
);
SELECT demo_delete_by_ids(
    'user_narrative_investigation_progress',
    'user_id',
    ARRAY(SELECT id FROM legacy_analytics_seed_users)
);

DELETE FROM profiles
WHERE id IN (SELECT id FROM legacy_analytics_seed_users);
