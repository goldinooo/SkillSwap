-- Needs V10 (users table) to run first.


CREATE TABLE conversations (
    id          BIGSERIAL   PRIMARY KEY,
    user_a_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    user_b_id   UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_a_id, user_b_id),
    CHECK (user_a_id < user_b_id)
);


CREATE INDEX idx_conversations_user_b ON conversations (user_b_id);

CREATE TABLE messages (
    id               BIGSERIAL   PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    content          TEXT        NOT NULL CHECK (length(content) BETWEEN 1 AND 2000),
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    read_at          TIMESTAMPTZ
);
CREATE INDEX idx_messages_conversation ON messages (conversation_id, created_at);

CREATE TABLE notifications (
    id          BIGSERIAL   PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(40) NOT NULL,
    payload     JSONB,
    read_at     TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_notifications_user ON notifications (user_id, read_at);
