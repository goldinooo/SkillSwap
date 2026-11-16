package com.trendenden.backend.publicapi.dto;

import com.trendenden.backend.publicapi.entity.ApiKey;

import java.time.Instant;
import java.util.UUID;

/**
 * Key metadata, with no key material of any kind: not the key, not even its hash.
 * Publishing the hash would let anyone who read one listing test candidate keys offline.
 */
public record ApiKeyResponse(
		UUID id,
		String name,
		Instant createdAt,
		Instant revokedAt,
		boolean revoked
) {

	public static ApiKeyResponse from(ApiKey key) {
		return new ApiKeyResponse(key.getId(), key.getName(), key.getCreatedAt(),
				key.getRevokedAt(), key.isRevoked());
	}
}
