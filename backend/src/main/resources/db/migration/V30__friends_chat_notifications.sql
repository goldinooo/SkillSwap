-- V30: friends, chat, notifications (Backend 3 - Nisrine)
-- Needs V10 (users table) to run first.

CREATE TABLE friendships (
    id            BIGSERIAL   PRIMARY KEY,
    requester_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    addressee_id  UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'ACCEPTED', 'BLOCKED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (requester_id <> addressee_id)
);

-- One row per pair, in either direction (A->B and B->A cannot both exist)
CREATE UNIQUE INDEX uq_friendships_pair
    ON friendships (LEAST(requester_id, addressee_id), GREATEST(requester_id, addressee_id));
CREATE INDEX idx_friendships_requester ON friendships (requester_id, status);
CREATE INDEX idx_friendships_addressee ON friendships (addressee_id, status);

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
    conversation_id  BIGSERIAL   NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
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
