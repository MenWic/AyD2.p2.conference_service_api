ALTER TABLE activities
    ALTER COLUMN type TYPE VARCHAR(20) USING type::text;

ALTER TABLE activities
    ADD CONSTRAINT ck_activity_type
    CHECK (type IN ('PONENCIA', 'TALLER'));

ALTER TABLE activity_leaders
    ALTER COLUMN leader_type TYPE VARCHAR(30) USING leader_type::text;

ALTER TABLE activity_leaders
    ADD CONSTRAINT ck_activity_leader_type
    CHECK (leader_type IN ('SPEAKER', 'WORKSHOP_LEADER', 'GUEST_SPEAKER'));

DROP TYPE IF EXISTS activity_leader_type;
