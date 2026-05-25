CREATE UNIQUE INDEX IF NOT EXISTS uq_diploma_participation
    ON diplomas (user_id, congress_id)
    WHERE type = 'PARTICIPATION';

CREATE UNIQUE INDEX IF NOT EXISTS uq_diploma_leadership
    ON diplomas (user_id, congress_id, activity_id)
    WHERE type = 'LEADERSHIP';
