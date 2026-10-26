-- Remove only the deterministic 120-account analytics seed introduced by V72.
-- Heritage content is deliberately untouched. The older named platform/demo
-- accounts are handled by the reviewed cleanup tool after a real admin exists,
-- so ownership can be transferred safely instead of guessed during migration.

CREATE TEMP TABLE legacy_analytics_seed_users ON COMMIT DROP AS
SELECT id
FROM profiles
WHERE email LIKE 'seed-analytics-%@histar.vn';

DELETE FROM pilot_events
WHERE user_id IN (SELECT id FROM legacy_analytics_seed_users)
   OR payload_json LIKE '%"seed":"admin-analytics-cu-chi"%';

DELETE FROM analytics_events
WHERE metadata = 'seed-admin-analytics-cu-chi'
   OR user_id IN (SELECT id FROM legacy_analytics_seed_users);

-- All remaining user-owned rows installed since V72 either cascade from
-- profiles or are removed explicitly here when their historical FK did not.
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
