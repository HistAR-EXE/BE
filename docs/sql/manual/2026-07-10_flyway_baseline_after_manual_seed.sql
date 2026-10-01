-- Mark Flyway schema as baseline V13 after manual seed (psql) already applied V1..V13.
-- Prevents Spring Boot from re-running all Flyway migrations on every Render cold start.

CREATE TABLE IF NOT EXISTS flyway_schema_history (
    installed_rank INTEGER NOT NULL,
    version VARCHAR(50),
    description VARCHAR(200) NOT NULL,
    type VARCHAR(20) NOT NULL,
    script VARCHAR(1000) NOT NULL,
    checksum INTEGER,
    installed_by VARCHAR(100) NOT NULL,
    installed_on TIMESTAMP NOT NULL DEFAULT NOW(),
    execution_time INTEGER NOT NULL,
    success BOOLEAN NOT NULL,
    PRIMARY KEY (installed_rank)
);

-- Fresh manual seed: baseline only when history is empty.
INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script,
    checksum, installed_by, installed_on, execution_time, success
)
SELECT
    1,
    '13',
    '<< Flyway Baseline >>',
    'BASELINE',
    '<< Flyway Baseline >>',
    NULL,
    'histar-manual-seed',
    NOW(),
    0,
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM flyway_schema_history);

-- Repair old boots that baseline at 0 and keep retrying V1..V13 on every deploy.
DELETE FROM flyway_schema_history
WHERE type = 'SQL'
  AND version IS NOT NULL
  AND version::INTEGER <= 13
  AND NOT EXISTS (
      SELECT 1
      FROM flyway_schema_history h2
      WHERE h2.type = 'BASELINE'
        AND h2.version = '13'
        AND h2.success = TRUE
  );

INSERT INTO flyway_schema_history (
    installed_rank, version, description, type, script,
    checksum, installed_by, installed_on, execution_time, success
)
SELECT
    COALESCE((SELECT MAX(installed_rank) FROM flyway_schema_history), 0) + 1,
    '13',
    '<< Flyway Baseline >>',
    'BASELINE',
    '<< Flyway Baseline >>',
    NULL,
    'histar-manual-seed-repair',
    NOW(),
    0,
    TRUE
WHERE NOT EXISTS (
    SELECT 1
    FROM flyway_schema_history
    WHERE type = 'BASELINE'
      AND version = '13'
      AND success = TRUE
);
