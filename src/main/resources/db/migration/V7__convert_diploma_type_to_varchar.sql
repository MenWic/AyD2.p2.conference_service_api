DROP INDEX IF EXISTS uq_diploma_participation;
DROP INDEX IF EXISTS uq_diploma_leadership;

ALTER TABLE diplomas
    DROP CONSTRAINT IF EXISTS ck_diploma_leadership;

ALTER TABLE diplomas
    DROP CONSTRAINT IF EXISTS ck_diploma_type;

ALTER TABLE diplomas
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

ALTER TABLE diplomas
    ADD CONSTRAINT ck_diploma_type
    CHECK (type IN ('PARTICIPATION', 'LEADERSHIP'));

ALTER TABLE diplomas
    ADD CONSTRAINT ck_diploma_leadership
    CHECK (
        (type = 'LEADERSHIP' AND activity_id IS NOT NULL)
        OR
        (type = 'PARTICIPATION' AND activity_id IS NULL)
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_diploma_participation
    ON diplomas (user_id, congress_id)
    WHERE type = 'PARTICIPATION';

CREATE UNIQUE INDEX IF NOT EXISTS uq_diploma_leadership
    ON diplomas (user_id, congress_id, activity_id)
    WHERE type = 'LEADERSHIP';

DROP TYPE IF EXISTS diploma_type;
