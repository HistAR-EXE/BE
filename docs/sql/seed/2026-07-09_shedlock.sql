-- ShedLock table for multi-instance scheduled jobs (Render / prod Postgres).
-- Also applied via Flyway: classpath:db/migration/V11__shedlock.sql

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL,
    lock_until TIMESTAMP NOT NULL,
    locked_at TIMESTAMP NOT NULL,
    locked_by VARCHAR(255) NOT NULL,
    PRIMARY KEY (name)
);
