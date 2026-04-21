ALTER TABLE selective.question_paper
    ADD COLUMN IF NOT EXISTS status VARCHAR(20);

UPDATE selective.question_paper
SET status = 'DRAFT'
WHERE status IS NULL;

ALTER TABLE selective.question_paper
    ALTER COLUMN status SET DEFAULT 'DRAFT';

ALTER TABLE selective.question_paper
    ALTER COLUMN status SET NOT NULL;

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'chk_question_paper_status'
              AND conrelid = 'selective.question_paper'::regclass
        ) THEN
            ALTER TABLE selective.question_paper
                ADD CONSTRAINT chk_question_paper_status
                    CHECK (status IN ('FINAL', 'DRAFT'));
        END IF;
    END
$$;
