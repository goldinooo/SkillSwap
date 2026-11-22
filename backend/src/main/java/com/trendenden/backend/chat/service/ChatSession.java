package com.trendenden.backend.chat.service;

import java.util.UUID;

// TEMPORARY: until Retahri's Session is merged.
// Only what chat needs from a session: who is in it and its status.
public record ChatSession(Long id, UUID teacherId, UUID learnerId, String status) {}
