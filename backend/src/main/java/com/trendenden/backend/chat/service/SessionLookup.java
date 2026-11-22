package com.trendenden.backend.chat.service;

import java.util.Optional;

// TEMPORARY: until Retahri's Session is merged.
// find(sessionId): the session if it exists, empty if not.
public interface SessionLookup {
    Optional<ChatSession> find(Long sessionId);
}
