CREATE TABLE IF NOT EXISTS selective.tag
(
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity     VARCHAR(50) NOT NULL CHECK (entity IN ('question')),
    key        VARCHAR(50) NOT NULL,
    value      VARCHAR(50) NOT NULL,
    created_at TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_tag_entity_key_value UNIQUE (entity, key, value)
);

ALTER TABLE selective.question
    ADD COLUMN IF NOT EXISTS tags JSON;

CREATE OR REPLACE FUNCTION selective.question_tags_are_valid(tags_json JSON)
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

    IF json_typeof(tags_json) <> 'array' THEN
        RETURN FALSE;
    END IF;

    FOR tag_text IN
        SELECT json_array_elements_text(tags_json)
        LOOP
            IF tag_text !~* '^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$' THEN
                RETURN FALSE;
            END IF;

            IF NOT EXISTS (
                SELECT 1
                FROM selective.tag tag
                WHERE tag.id = tag_text::UUID
                  AND tag.entity = 'question'
            ) THEN
                RETURN FALSE;
            END IF;
        END LOOP;

    RETURN TRUE;
END;
$$;

ALTER TABLE selective.question
    ADD CONSTRAINT chk_question_tags_valid
        CHECK (selective.question_tags_are_valid(tags));
