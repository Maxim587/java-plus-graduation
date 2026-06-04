DROP TABLE IF EXISTS comments;

CREATE TABLE IF NOT EXISTS comments (
    id              BIGINT GENERATED ALWAYS AS IDENTITY (START WITH 0 MINVALUE 0) PRIMARY KEY,
    text            VARCHAR(2000) NOT NULL,
    event_id        BIGINT NOT NULL,
    author_id       BIGINT NOT NULL,
    created_at      TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status          VARCHAR(100) NOT NULL
);

CREATE INDEX idx_comments_event_id ON comments (event_id);
CREATE INDEX idx_comments_author_id ON comments (author_id);