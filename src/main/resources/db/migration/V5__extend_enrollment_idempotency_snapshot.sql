ALTER TABLE enrollment_idempotency_records
    ADD COLUMN institution_id UUID,
    ADD COLUMN congress_name_snapshot VARCHAR(255),
    ADD COLUMN institution_name_snapshot VARCHAR(255),
    ADD COLUMN amount NUMERIC(12,2);

UPDATE enrollment_idempotency_records r
SET institution_id = c.institution_id,
    congress_name_snapshot = c.name,
    institution_name_snapshot = i.name,
    amount = c.price
FROM congresses c
         JOIN institutions i ON i.id = c.institution_id
WHERE r.congress_id = c.id
  AND (
    r.institution_id IS NULL
        OR r.congress_name_snapshot IS NULL
        OR r.institution_name_snapshot IS NULL
        OR r.amount IS NULL
    );

ALTER TABLE enrollment_idempotency_records
    ALTER COLUMN institution_id SET NOT NULL,
    ALTER COLUMN congress_name_snapshot SET NOT NULL,
    ALTER COLUMN institution_name_snapshot SET NOT NULL,
    ALTER COLUMN amount SET NOT NULL;
