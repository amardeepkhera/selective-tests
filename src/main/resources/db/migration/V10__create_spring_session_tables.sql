CREATE TABLE IF NOT EXISTS selective.spring_session
(
    primary_id            CHAR(36)    NOT NULL,
    session_id            CHAR(36)    NOT NULL,
    creation_time         BIGINT      NOT NULL,
    last_access_time      BIGINT      NOT NULL,
    max_inactive_interval INT         NOT NULL,
    expiry_time           BIGINT      NOT NULL,
    principal_name        VARCHAR(100),
    CONSTRAINT spring_session_pk PRIMARY KEY (primary_id),
    CONSTRAINT spring_session_session_id_uk UNIQUE (session_id)
);

CREATE INDEX IF NOT EXISTS spring_session_expiry_time_ix
    ON selective.spring_session (expiry_time);

CREATE INDEX IF NOT EXISTS spring_session_principal_name_ix
    ON selective.spring_session (principal_name);

CREATE TABLE IF NOT EXISTS selective.spring_session_attributes
(
    session_primary_id CHAR(36)     NOT NULL,
    attribute_name     VARCHAR(200) NOT NULL,
    attribute_bytes    BYTEA        NOT NULL,
    CONSTRAINT spring_session_attributes_pk PRIMARY KEY (session_primary_id, attribute_name),
    CONSTRAINT spring_session_attributes_fk FOREIGN KEY (session_primary_id)
        REFERENCES selective.spring_session (primary_id) ON DELETE CASCADE
);
