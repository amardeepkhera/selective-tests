CREATE TABLE IF NOT EXISTS selective.subject
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name       varchar(50) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE selective.test
    ADD COLUMN IF NOT EXISTS name varchar(100);

ALTER TABLE selective.test
    ADD COLUMN IF NOT EXISTS subject_id UUID;

UPDATE selective.test t
SET name = COALESCE(NULLIF(trim(t.name), ''), t.subject),
    subject_id = s.id
FROM selective.subject s
WHERE s.name = t.subject
  AND (t.name IS NULL OR t.subject_id IS NULL);

ALTER TABLE selective.test
    ALTER COLUMN name SET NOT NULL;

ALTER TABLE selective.test
    ALTER COLUMN subject_id SET NOT NULL;

ALTER TABLE selective.test
    ADD CONSTRAINT test_subject_fk
        FOREIGN KEY (subject_id) REFERENCES selective.subject (id);

ALTER TABLE selective.test
    DROP COLUMN IF EXISTS subject;
