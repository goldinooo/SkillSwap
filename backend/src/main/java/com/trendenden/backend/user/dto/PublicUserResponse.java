package com.trendenden.backend.user.dto;

import com.trendenden.backend.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * What anyone else sees. Separate from {@link UserResponse} so that adding a field for the
 * owner's own view cannot accidentally publish it to the world.
 *
 * <p>No email: it is personal data and nothing in the product needs another user's address.
 * No role either, since who the admins are is not public information.
 */
public record PublicUserResponse(
		UUID id,
		String username,
		String displayName,
		String bio,
		String avatarUrl,
		String timezone,
		Instant createdAt
) {

	public static PublicUserResponse from(User user) {
		return new PublicUserResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getBio(),
				user.getAvatarUrl(),
				user.getTimezone(),
				user.getCreatedAt()
		);
	}
}
