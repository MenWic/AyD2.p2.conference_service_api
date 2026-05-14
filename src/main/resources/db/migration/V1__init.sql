CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

CREATE TYPE activity_type AS ENUM ('PONENCIA', 'TALLER');
CREATE TYPE call_status AS ENUM ('OPEN', 'CLOSED');
CREATE TYPE proposal_status AS ENUM ('PENDING', 'APPROVED', 'REJECTED');
CREATE TYPE diploma_type AS ENUM ('PARTICIPATION', 'LEADERSHIP');
CREATE TYPE activity_leader_type AS ENUM ('SPEAKER', 'WORKSHOP_LEADER', 'GUEST_SPEAKER');

CREATE TABLE institutions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    contact_email VARCHAR(320) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT uq_institutions_name UNIQUE (name)
);

CREATE TABLE congresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    institution_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location VARCHAR(500) NOT NULL,
    price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT ck_congress_price CHECK (price >= 35.00),
    CONSTRAINT ck_congress_dates CHECK (start_date <= end_date),
    CONSTRAINT fk_congress_institution FOREIGN KEY (institution_id)
        REFERENCES institutions (id) ON DELETE RESTRICT
);

CREATE INDEX idx_congress_institution ON congresses (institution_id);
CREATE INDEX idx_congress_start_date ON congresses (start_date);

CREATE TABLE rooms (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    congress_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    capacity INT,
    location VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT uq_rooms_name_per_congress UNIQUE (congress_id, name),
    CONSTRAINT ck_room_capacity CHECK (capacity IS NULL OR capacity > 0),
    CONSTRAINT fk_room_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT
);

CREATE TABLE activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    congress_id UUID NOT NULL,
    room_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    type activity_type NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    workshop_capacity INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT ck_activity_times CHECK (start_time < end_time),
    CONSTRAINT ck_workshop_capacity CHECK (
        (type = 'TALLER' AND workshop_capacity IS NOT NULL AND workshop_capacity > 0)
        OR
        (type = 'PONENCIA' AND workshop_capacity IS NULL)
    ),
    CONSTRAINT fk_activity_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_activity_room FOREIGN KEY (room_id)
        REFERENCES rooms (id) ON DELETE RESTRICT
);

CREATE INDEX idx_activity_congress ON activities (congress_id);
CREATE INDEX idx_activity_room ON activities (room_id);
CREATE INDEX idx_activity_times ON activities (room_id, start_time, end_time);

ALTER TABLE activities
    ADD CONSTRAINT excl_room_no_overlap
    EXCLUDE USING gist (
        room_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    );

CREATE TABLE activity_leaders (
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL,
    leader_type activity_leader_type NOT NULL,
    PRIMARY KEY (activity_id, user_id),
    CONSTRAINT fk_leader_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE CASCADE
);

CREATE TABLE calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    congress_id UUID NOT NULL,
    status call_status NOT NULL DEFAULT 'OPEN',
    opened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at TIMESTAMPTZ,
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT ck_call_closed_at CHECK (
        (status = 'OPEN' AND closed_at IS NULL)
        OR
        (status = 'CLOSED' AND closed_at IS NOT NULL)
    ),
    CONSTRAINT fk_call_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT
);

CREATE INDEX idx_call_congress ON calls (congress_id);

CREATE TABLE proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_id UUID NOT NULL,
    author_user_id UUID NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    type activity_type NOT NULL,
    status proposal_status NOT NULL DEFAULT 'PENDING',
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    created_activity_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    updated_by UUID,
    CONSTRAINT ck_proposal_reviewed CHECK (
        (status = 'PENDING' AND reviewed_by IS NULL AND reviewed_at IS NULL)
        OR
        (status IN ('APPROVED', 'REJECTED') AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)
    ),
    CONSTRAINT fk_proposal_call FOREIGN KEY (call_id)
        REFERENCES calls (id) ON DELETE RESTRICT,
    CONSTRAINT fk_proposal_created_activity FOREIGN KEY (created_activity_id)
        REFERENCES activities (id) ON DELETE RESTRICT,
    CONSTRAINT ck_proposal_created_activity CHECK (
        created_activity_id IS NULL OR status = 'APPROVED'
    )
);

CREATE INDEX idx_proposal_call ON proposals (call_id);
CREATE INDEX idx_proposal_author ON proposals (author_user_id);
CREATE INDEX idx_proposal_created_activity ON proposals (created_activity_id);

CREATE TABLE committee_members (
    congress_id UUID NOT NULL,
    user_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    added_by UUID NOT NULL,
    PRIMARY KEY (congress_id, user_id),
    CONSTRAINT fk_cm_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT
);

CREATE TABLE enrollments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    congress_id UUID NOT NULL,
    user_id UUID NOT NULL,
    payment_id UUID NOT NULL,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    payment_date DATE NOT NULL,
    created_by UUID NOT NULL,
    CONSTRAINT uq_enrollment UNIQUE (congress_id, user_id),
    CONSTRAINT fk_enrollment_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT
);

CREATE INDEX idx_enrollment_congress ON enrollments (congress_id);
CREATE INDEX idx_enrollment_user ON enrollments (user_id);

CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL,
    reserved_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    CONSTRAINT uq_reservation UNIQUE (activity_id, user_id),
    CONSTRAINT fk_reservation_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE RESTRICT
);

CREATE INDEX idx_reservation_activity ON reservations (activity_id);
CREATE INDEX idx_reservation_user ON reservations (user_id);

CREATE TABLE attendances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    activity_id UUID NOT NULL,
    user_id UUID NOT NULL,
    personal_id_snapshot VARCHAR(50) NOT NULL,
    registered_by UUID NOT NULL,
    registered_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_attendance_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE RESTRICT,
    CONSTRAINT uq_attendance_activity_user UNIQUE (activity_id, user_id)
);

CREATE INDEX idx_attendance_activity ON attendances (activity_id);
CREATE INDEX idx_attendance_user ON attendances (user_id);
CREATE INDEX idx_attendance_personal_id ON attendances (personal_id_snapshot);

CREATE TABLE diplomas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    congress_id UUID NOT NULL,
    type diploma_type NOT NULL,
    activity_id UUID,
    issued_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_by UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_diploma_leadership CHECK (
        (type = 'LEADERSHIP' AND activity_id IS NOT NULL)
        OR
        (type = 'PARTICIPATION' AND activity_id IS NULL)
    ),
    CONSTRAINT uq_diploma UNIQUE (user_id, congress_id, type, activity_id),
    CONSTRAINT fk_diploma_congress FOREIGN KEY (congress_id)
        REFERENCES congresses (id) ON DELETE RESTRICT,
    CONSTRAINT fk_diploma_activity FOREIGN KEY (activity_id)
        REFERENCES activities (id) ON DELETE RESTRICT
);

CREATE INDEX idx_diploma_user ON diplomas (user_id);
CREATE INDEX idx_diploma_congress ON diplomas (congress_id);
