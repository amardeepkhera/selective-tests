CREATE TABLE IF NOT EXISTS selective.question_paper
(
    id         UUID PRIMARY KEY   DEFAULT gen_random_uuid(),
    paper      JSON      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE selective.question
    DROP COLUMN IF EXISTS created_by;

ALTER TABLE selective.question
    DROP COLUMN IF EXISTS updated_at;

CREATE TABLE IF NOT EXISTS selective.test
(
    id                UUID PRIMARY KEY                                       DEFAULT gen_random_uuid(),
    subject           varchar(50)                                   NOT NULL,
    duration_mins     int                                           NOT NULL,
    instructions      text,
    question_paper_id UUID REFERENCES selective.question_paper (id) NOT NULL,
    created_at        TIMESTAMP                                     NOT NULL DEFAULT CURRENT_TIMESTAMP
);