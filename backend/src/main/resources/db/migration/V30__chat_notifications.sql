

CREATE TABLE messages (
    id               BIGSERIAL   PRIMARY KEY,
    session_id       BIGINT  NOT NULL REFERENCES sessions(id) ON DELETE CASCADE,
    sender_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content          TEXT        NOT NULL CHECK (length(content) BETWEEN 1 AND 2000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at          TIMESTAMPTZ
);
CREATE INDEX idx_messages_session ON messages (session_id, created_at);

CREATE TABLE notifications (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,
    payload     JSONB,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, read_at);
