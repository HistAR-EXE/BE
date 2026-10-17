-- Admin Analytics demo seed for Äá»‹a Ä‘áº¡o Cá»§ Chi (location 11111111-...).
-- Proportions inspired by Stitch AnalyticsView mock, scaled for local DB (K-anonymity threshold = 10).
-- Idempotent: removes prior seed-analytics-* rows then re-inserts.

-- Applied inside Flyway transaction.

-- Session-scoped TEMP (no ON COMMIT DROP): bootstrap runs via psql autocommit;
-- ON COMMIT DROP would drop the table before the next statement.
CREATE TEMP TABLE seed_analytics_users (
    idx int PRIMARY KEY,
    user_id uuid NOT NULL,
    email text NOT NULL
);

INSERT INTO seed_analytics_users (idx, user_id, email)
SELECT
    g,
    ('a1000000-0000-4000-8000-' || lpad(to_hex(g), 12, '0'))::uuid,
    'seed-analytics-' || lpad(g::text, 3, '0') || '@histar.vn'
FROM generate_series(1, 120) AS g;

-- Cleanup previous seed (FK-safe order)
DELETE FROM visit_session_events
WHERE visit_session_id IN (
    SELECT vs.id FROM visit_sessions vs
    JOIN seed_analytics_users u ON u.user_id = vs.user_id
);
DELETE FROM analytics_events
WHERE user_id IN (SELECT user_id FROM seed_analytics_users)
   OR metadata = 'seed-admin-analytics-cu-chi';
DELETE FROM visit_sessions
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM pilot_events
WHERE user_id IN (SELECT user_id FROM seed_analytics_users)
   OR payload_json LIKE '%"seed":"admin-analytics-cu-chi"%';
DELETE FROM checkins
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM user_discoveries
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM user_quest_progress
WHERE user_id IN (SELECT user_id FROM seed_analytics_users);
DELETE FROM profiles
WHERE email LIKE 'seed-analytics-%@histar.vn'
   OR id IN (SELECT user_id FROM seed_analytics_users);

INSERT INTO profiles (id, email, password_hash, provider, role, tier, display_name, level, total_points, city, created_at, email_verified)
SELECT
    u.user_id,
    u.email,
    crypt('Demo@2026', gen_salt('bf', 4)),
    'local',
    'USER',
    CASE WHEN u.idx <= 40 THEN 'PREMIUM' ELSE 'FREE' END,
    'Seed Analytics ' || lpad(u.idx::text, 3, '0'),
    1 + (u.idx % 8),
    100 + u.idx * 17,
    'TP.HCM',
    now() - ((u.idx % 25) || ' days')::interval,
    true
FROM seed_analytics_users u;

-- Closed visit sessions (~14â€“22 min) with discovery events for drop-off + session quality
WITH loc AS (
    SELECT '11111111-1111-1111-1111-111111111111'::uuid AS id
),
pois AS (
    SELECT unlock_key, name, sort_order,
           row_number() OVER (ORDER BY sort_order) AS rn
    FROM discovery_points
    WHERE location_id = '11111111-1111-1111-1111-111111111111'
      AND deleted_at IS NULL
),
sessions AS (
    INSERT INTO visit_sessions (
        id, user_id, location_id, mode, started_at, ended_at, status, last_activity_at, ended_reason
    )
    SELECT
        ('b2000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
        u.user_id,
        loc.id,
        CASE WHEN u.idx % 3 = 0 THEN 'onsite' ELSE 'online' END,
        now() - ((u.idx % 28) || ' days')::interval - interval '90 minutes',
        now() - ((u.idx % 28) || ' days')::interval - interval '90 minutes'
            + ((14 + (u.idx % 9)) || ' minutes')::interval,
        'CLOSED',
        now() - ((u.idx % 28) || ' days')::interval - interval '70 minutes',
        'COMPLETED'
    FROM seed_analytics_users u
    CROSS JOIN loc
    RETURNING id, user_id
)
INSERT INTO visit_session_events (id, visit_session_id, event_type, event_key, source, created_at, poi_name_snapshot)
SELECT
    gen_random_uuid(),
    s.id,
    'discovery',
    p.unlock_key,
    'seed-admin-analytics',
    now() - ((su.idx % 28) || ' days')::interval - interval '80 minutes' + (p.rn || ' minutes')::interval,
    p.name
FROM sessions s
JOIN seed_analytics_users su ON su.user_id = s.user_id
JOIN pois p ON p.rn <= CASE
    WHEN su.idx <= 100 THEN 6
    WHEN su.idx <= 110 THEN 4
    ELSE 2
END;

-- user_discoveries (POI unlock ranking) â€” top POIs get more distinct users
INSERT INTO user_discoveries (user_id, discovery_key, discovered_at, location_id)
SELECT
    u.user_id,
    p.unlock_key,
    now() - ((u.idx % 27) || ' days')::interval - (p.sort_order || ' hours')::interval,
    '11111111-1111-1111-1111-111111111111'::uuid
FROM seed_analytics_users u
JOIN discovery_points p
  ON p.location_id = '11111111-1111-1111-1111-111111111111'
 AND p.deleted_at IS NULL
WHERE
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222222' AND u.idx <= 110) OR -- Cá»•ng vÃ o ~92%
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222221' AND u.idx <= 95) OR  -- Báº¿p
    (p.unlock_key = 'scene:22222222-2222-2222-2222-222222222223' AND u.idx <= 82) OR  -- PhÃ²ng há»p
    (p.unlock_key = 'photo:cua-ham' AND u.idx <= 70) OR
    (p.unlock_key = 'photo:gieng' AND u.idx <= 58) OR
    (p.unlock_key = 'artifact:vu-khi' AND u.idx <= 48) OR
    (p.unlock_key = 'artifact:tram-xa' AND u.idx <= 40) OR
    (p.unlock_key = 'hotspot:vent' AND u.idx <= 28) OR
    (p.unlock_key = 'artifact:bay' AND u.idx <= 22);

-- Quest funnel (started vs completed) â€” rates ~100 / 86 / 71 / 68 / 65 / 60 / 55 of base 120
INSERT INTO user_quest_progress (
    id, user_id, quest_id, status, started_at, completed_at, location_id, current_step, steps_total
)
SELECT
    gen_random_uuid(),
    u.user_id,
    q.id,
    CASE
        WHEN u.idx <= q.complete_upto THEN 'COMPLETED'
        WHEN u.idx <= q.start_upto THEN 'IN_PROGRESS'
        ELSE 'NOT_STARTED'
    END,
    CASE WHEN u.idx <= q.start_upto
        THEN now() - ((u.idx % 26) || ' days')::interval ELSE NULL END,
    CASE WHEN u.idx <= q.complete_upto
        THEN now() - ((u.idx % 26) || ' days')::interval + interval '40 minutes' ELSE NULL END,
    '11111111-1111-1111-1111-111111111111'::uuid,
    CASE WHEN u.idx <= q.complete_upto THEN 3 WHEN u.idx <= q.start_upto THEN 1 ELSE 0 END,
    3
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('33333333-3333-3333-3333-333333333335'::uuid, 120, 104), -- Ãnh sÃ¡ng ÄÃ¨n MÃ¹ U
        ('33333333-3333-3333-3333-333333333339'::uuid, 110, 86),  -- Ba táº§ng lÃ²ng Ä‘áº¥t
        ('33333333-3333-3333-3333-333333333337'::uuid, 100, 72),  -- Báº¿p khÃ´ng khÃ³i
        ('33333333-3333-3333-3333-333333333333'::uuid, 95, 65),   -- HÃ nh trÃ¬nh dÆ°á»›i lÃ²ng Ä‘áº¥t
        ('33333333-3333-3333-3333-333333333336'::uuid, 90, 58),   -- Máº­t lá»‡nh thá»­ game
        ('33333333-3333-3333-3333-333333333334'::uuid, 85, 52),   -- Máº­t mÃ£ Sa bÃ n
        ('33333333-3333-3333-3333-333333333338'::uuid, 80, 45)    -- Náº¯p háº§m vÃ  cáº¡m báº«y
) AS q(id, start_upto, complete_upto)
WHERE u.idx <= q.start_upto;

-- Online â†’ onsite conversion (~32% of discoverers also check in after discovery)
INSERT INTO checkins (id, user_id, location_id, latitude, longitude, created_at, presence_method)
SELECT
    ('c3000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    11.1438 + (u.idx * 0.00001),
    106.4631 + (u.idx * 0.00001),
    now() - ((u.idx % 25) || ' days')::interval + interval '2 hours',
    'GPS'
FROM seed_analytics_users u
WHERE u.idx <= 38;

-- analytics_events for KPI activeUsers / eventCount / activity-series / quest_completed
INSERT INTO analytics_events (
    id, user_id, location_id, visit_session_id, event_type, event_key, source, created_at, metadata
)
SELECT
    gen_random_uuid(),
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    ('b2000000-0000-4000-8000-' || lpad(to_hex(u.idx), 12, '0'))::uuid,
    e.event_type,
    e.event_key,
    'seed-admin-analytics',
    -- Bias weekends (Sat/Sun) higher density like Stitch heatmap
    date_trunc('day', now()) - ((u.idx + e.day_offset) % 28 || ' days')::interval
        + ((9 + (u.idx % 7)) || ' hours')::interval
        + ((u.idx % 50) || ' minutes')::interval,
    'seed-admin-analytics-cu-chi'
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('session_start', 'visit', 0),
        ('session_end', 'visit', 0),
        ('discovery', 'poi', 1),
        ('quest_started', 'quest', 2),
        ('quest_completed', 'quest', 3),
        ('checkin', 'gps', 4)
) AS e(event_type, event_key, day_offset)
WHERE NOT (e.event_type = 'quest_completed' AND u.idx > 78)
  AND NOT (e.event_type = 'checkin' AND u.idx > 38);

-- Extra weekdayÃ—hour events for Stitch heatmap peaks (09:30 / 14:30) and weekend denser cells
INSERT INTO analytics_events (
    id, user_id, location_id, event_type, event_key, source, created_at, metadata
)
SELECT
    gen_random_uuid(),
    u.user_id,
    '11111111-1111-1111-1111-111111111111'::uuid,
    'session_start',
    'visit',
    'seed-admin-analytics',
    date_trunc('week', now())
      + ((d.dow) || ' days')::interval
      + ((h.hour) || ' hours')::interval
      + ((u.idx % 40) || ' minutes')::interval,
    'seed-admin-analytics-cu-chi'
FROM seed_analytics_users u
CROSS JOIN (VALUES (0), (1), (2), (3), (4), (5), (6)) AS d(dow)
CROSS JOIN (VALUES (8), (9), (10), (11), (13), (14), (15), (16)) AS h(hour)
WHERE u.idx <= CASE
    WHEN h.hour IN (9, 10, 14, 15) AND d.dow IN (5, 6) THEN 110
    WHEN h.hour IN (9, 10, 14, 15) THEN 85
    WHEN d.dow IN (5, 6) THEN 55
    ELSE 28
END;

-- Pilot KPI attribution (last 7 days) for "Nguá»“n thu hÃºt"
INSERT INTO pilot_events (id, client_uuid, user_id, session_id, event_type, station_code, payload_json, occurred_at, created_at)
SELECT
    gen_random_uuid(),
    gen_random_uuid(),
    u.user_id,
    gen_random_uuid(),
    e.event_type,
    NULL,
    '{"seed":"admin-analytics-cu-chi"}',
    now() - ((u.idx % 6) || ' days')::interval - ((u.idx % 8) || ' hours')::interval,
    now()
FROM seed_analytics_users u
CROSS JOIN (
    VALUES
        ('landing_visit', 90),
        ('pack_loaded', 70),
        ('share_initiated', 35),
        ('checkin_result', 38),
        ('nps_submitted', 25),
        ('session_start', 100),
        ('session_end', 95)
) AS e(event_type, max_idx)
WHERE u.idx <= e.max_idx;

