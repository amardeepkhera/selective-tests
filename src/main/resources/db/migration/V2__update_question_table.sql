ALTER TABLE selective.question
    DROP COLUMN IF EXISTS updated_by;

ALTER TABLE selective.question
    ADD COLUMN IF NOT EXISTS options JSON NOT NULL;

ALTER TABLE selective.question
    ADD COLUMN IF NOT EXISTS answer VARCHAR(5) NOT NULL;
