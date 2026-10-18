-- Read-only checks. Run against the backend's actual production database.
-- No user data, credentials, or row-level progress is returned.
BEGIN TRANSACTION READ ONLY;

SELECT current_database() AS database_name, current_schema() AS schema_name;

SELECT to_regclass('public.flyway_schema_history') IS NOT NULL AS has_flyway_history,
       to_regclass('public.xp_ledger') IS NOT NULL AS has_xp_ledger,
       to_regclass('public.explore_attempts') IS NOT NULL AS has_explore_attempts;

SELECT EXISTS (
    SELECT 1 FROM information_schema.columns
    WHERE table_schema = 'public' AND table_name = 'messages'
      AND column_name = 'server_generated'
) AS has_verified_message_column;

-- V74 and V75 must both have completed successfully. Run this SELECT only
-- when has_flyway_history above is true.
SELECT version, description, script, success
FROM public.flyway_schema_history
ORDER BY installed_rank DESC LIMIT 12;

SELECT count(*) AS explore_badge_definitions
FROM public.badges
WHERE id IN (
    'b0000001-0000-4000-8000-000000000201',
    'b0000001-0000-4000-8000-000000000202',
    'b0000001-0000-4000-8000-000000000203',
    'b0000001-0000-4000-8000-000000000204',
    'b0000001-0000-4000-8000-000000000205',
    'b0000001-0000-4000-8000-000000000206',
    'b0000001-0000-4000-8000-000000000207',
    'b0000001-0000-4000-8000-000000000208',
    'b0000001-0000-4000-8000-000000000209'
);

ROLLBACK;
