-- Guard de idempotencia para POST /congresses/{id}/enrollments
CREATE TABLE enrollment_idempotency_records (
    idempotency_key VARCHAR(120) PRIMARY KEY,
    congress_id UUID NOT NULL,
    user_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    enrollment_id UUID,
    payment_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_idempotency_status CHECK (
        status IN (
            'PROCESSING',
            'SUCCEEDED',
            'FAILED'
        )
    ),
    CONSTRAINT ck_idempotency_enrollment CHECK (
        (
            status = 'SUCCEEDED'
            AND enrollment_id IS NOT NULL
            AND payment_id IS NOT NULL
        )
        OR (status != 'SUCCEEDED')
    )
);

-- Unicidad de inscripcion activa (en curso o exitosa) para el mismo congreso y usuario.
-- Permite multiples FAILED con distintas idempotency_keys para el mismo par (congress,user).
CREATE UNIQUE INDEX uq_idempotency_active_enrollment ON enrollment_idempotency_records (congress_id, user_id)
WHERE
    status IN ('PROCESSING', 'SUCCEEDED');

CREATE INDEX idx_idempotency_congress_user ON enrollment_idempotency_records (congress_id, user_id);

CREATE INDEX idx_idempotency_created ON enrollment_idempotency_records (created_at);