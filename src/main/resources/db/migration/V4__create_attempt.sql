CREATE TABLE IF NOT EXISTS selective.attempt
(
    id           UUID PRIMARY KEY                   DEFAULT gen_random_uuid(),
    test_id      UUID REFERENCES selective.test (id) NOT NULL,
    answers      JSONB,
    status       VARCHAR(20) NOT NULL              DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    started_at   TIMESTAMP NOT NULL                DEFAULT CURRENT_TIMESTAMP,
    submitted_at TIMESTAMP
);
