package com.trendenden.backend.publicapi.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * The only response that ever contains the key itself.
 *
 * @param key the raw key. Shown once: only its SHA-256 digest is stored, so it cannot be
 *            recovered or re-displayed. A lost key has to be revoked and replaced.
 */
public record ApiKeyCreatedResponse(UUID id, String name, String key, Instant createdAt) {
}
