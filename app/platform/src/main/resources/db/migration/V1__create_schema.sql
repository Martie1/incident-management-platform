CREATE TABLE users (
    id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name  VARCHAR(100) NOT NULL,
    email VARCHAR(254) NOT NULL
);

CREATE TABLE incidents (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       VARCHAR(200)  NOT NULL,
    description VARCHAR(2000),
    severity    VARCHAR(20)   NOT NULL,
    status      VARCHAR(20)   NOT NULL,
    assignee_id BIGINT REFERENCES users (id),
    created_at  TIMESTAMPTZ   NOT NULL,
    resolved_at TIMESTAMPTZ
);

CREATE INDEX incidents_assignee_id_idx ON incidents (assignee_id);
