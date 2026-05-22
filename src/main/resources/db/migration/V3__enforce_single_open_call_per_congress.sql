DROP INDEX IF EXISTS uq_open_call_per_congress;

ALTER TABLE calls
    DROP CONSTRAINT IF EXISTS ck_call_closed_at;

ALTER TABLE proposals
    DROP CONSTRAINT IF EXISTS ck_proposal_reviewed;

ALTER TABLE proposals
    DROP CONSTRAINT IF EXISTS ck_proposal_created_activity;

ALTER TABLE calls
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE proposals
    ALTER COLUMN status TYPE VARCHAR(20) USING status::text;

ALTER TABLE proposals
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

ALTER TABLE calls
    DROP CONSTRAINT IF EXISTS ck_call_status;

ALTER TABLE calls
    ADD CONSTRAINT ck_call_status
    CHECK (status IN ('OPEN', 'CLOSED'));

ALTER TABLE calls
    ADD CONSTRAINT ck_call_closed_at
    CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR
        (status = 'CLOSED' AND closed_at IS NOT NULL)
    );

ALTER TABLE proposals
    DROP CONSTRAINT IF EXISTS ck_proposal_status;

ALTER TABLE proposals
    ADD CONSTRAINT ck_proposal_status
    CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'));

ALTER TABLE proposals
    DROP CONSTRAINT IF EXISTS ck_proposal_type;

ALTER TABLE proposals
    ADD CONSTRAINT ck_proposal_type
    CHECK (type IN ('PONENCIA', 'TALLER'));

ALTER TABLE proposals
    ADD CONSTRAINT ck_proposal_reviewed
    CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR
        (status IN ('APPROVED', 'REJECTED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    );

ALTER TABLE proposals
    ADD CONSTRAINT ck_proposal_created_activity
    CHECK (
        created_activity_id IS NULL OR status = 'APPROVED'
    );

CREATE UNIQUE INDEX IF NOT EXISTS uq_open_call_per_congress
    ON calls (congress_id)
    WHERE status = 'OPEN';

DROP TYPE IF EXISTS call_status;
DROP TYPE IF EXISTS proposal_status;
DROP TYPE IF EXISTS activity_type;
