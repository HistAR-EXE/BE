-- V95 created era as SMALLINT, while UserCreation.era is a Java Integer.
-- Widening the column preserves all existing year values and satisfies
-- Hibernate schema validation without changing the already-applied V95.
ALTER TABLE user_creations
    ALTER COLUMN era TYPE integer USING era::integer;
