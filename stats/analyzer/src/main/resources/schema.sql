DROP TABLE IF EXISTS user_action;
DROP TABLE IF EXISTS event_similarity;


CREATE TABLE IF NOT EXISTS user_action (
    id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id   BIGINT           NOT NULL,
    event_id  BIGINT           NOT NULL,
    weight    DOUBLE PRECISION NOT NULL,
    timestamp TIMESTAMP        NOT NULL
);

CREATE TABLE IF NOT EXISTS event_similarity (
    id      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    event_a BIGINT           NOT NULL,
    event_b BIGINT           NOT NULL,
    score   DOUBLE PRECISION NOT NULL
);