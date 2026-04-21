ALTER TABLE selective.tag
    DROP CONSTRAINT IF EXISTS tag_entity_check;

ALTER TABLE selective.tag
    ADD CONSTRAINT tag_entity_check
        CHECK (entity IN ('question', 'question_paper'));

ALTER TABLE selective.question_paper
    ADD COLUMN IF NOT EXISTS tags JSONB;

CREATE OR REPLACE FUNCTION selective.question_paper_tags_are_valid(tags_json JSONB)
    RETURNS BOOLEAN
    LANGUAGE plpgsql
AS
$$
DECLARE
    tag_text TEXT;
BEGIN
    IF tags_json IS NULL THEN
        RETURN TRUE;
    END IF;

    IF jsonb_typeof(tags_json) <> 'array' THEN
        RETURN FALSE;
    END IF;

    FOR tag_text IN
        SELECT jsonb_array_elements_text(tags_json)
        LOOP
            IF tag_text !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$' THEN
                RETURN FALSE;
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM selective.tag tag
                WHERE tag.id = tag_text::UUID
                  AND tag.entity = 'question_paper'
            ) THEN
                RETURN FALSE;
            END IF;
        END LOOP;

    RETURN TRUE;
END;
$$;

ALTER TABLE selective.question_paper
    ADD CONSTRAINT chk_question_paper_tags_valid
        CHECK (selective.question_paper_tags_are_valid(tags));
