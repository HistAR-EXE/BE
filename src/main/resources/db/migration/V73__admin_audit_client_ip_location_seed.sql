-- Backfill clientIp + location into existing admin_audit_events metadata (demo / ops).
-- Skips rows that already have clientIp or ip. Cycles a small VN ISP sample set.

WITH ranked AS (
    SELECT
        id,
        ROW_NUMBER() OVER (ORDER BY occurred_at DESC, id) AS rn
    FROM admin_audit_events
    WHERE metadata_json IS NULL
       OR NOT (
            COALESCE(metadata_json::jsonb, '{}'::jsonb) ? 'clientIp'
            OR COALESCE(metadata_json::jsonb, '{}'::jsonb) ? 'ip'
       )
),
sample AS (
    SELECT *
    FROM (VALUES
        (0, '14.232.18.91',  'Hà Nội, VN'),
        (1, '118.70.211.42', 'Hà Nội, VN'),
        (2, '42.116.89.15',  'Đà Nẵng, VN'),
        (3, '113.161.72.8',  'TP. Hồ Chí Minh, VN'),
        (4, '171.224.178.33','Hà Nội, VN'),
        (5, '27.72.144.10',  'Hải Phòng, VN'),
        (6, '115.73.214.55', 'Cần Thơ, VN'),
        (7, '203.113.131.7', 'TP. Hồ Chí Minh, VN')
    ) AS t(slot, client_ip, location)
)
UPDATE admin_audit_events e
SET metadata_json = (
    COALESCE(e.metadata_json::jsonb, '{}'::jsonb)
    || jsonb_build_object(
        'clientIp', s.client_ip,
        'location', s.location
    )
)::text
FROM ranked r
JOIN sample s ON s.slot = ((r.rn - 1) % 8)
WHERE e.id = r.id;
