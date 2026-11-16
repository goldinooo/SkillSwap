package com.trendenden.backend.user.dto;

import com.trendenden.backend.common.security.Role;
import com.trendenden.backend.user.entity.User;

import java.time.Instant;
import java.util.UUID;

/**
 * What the owner of an account sees about themselves.
 *
 * <p>Deliberately missing: {@code passwordHash} and {@code twoFactorSecret}. Returning the
 * entity instead of this record would serialise both, which is why section 9 of
 * project_context.md forbids exposing entities.
 */
public record UserResponse(
		UUID id,
		String username,
		String displayName,
		String email,
		String firstName,
		String lastName,
		String bio,
		String avatarUrl,
		String timezone,
		Role role,
		boolean emailVerified,
		boolean twoFactorEnabled,
		Instant lastSeen,
		Instant createdAt
) {

	public static UserResponse from(User user) {
		return new UserResponse(
				user.getId(),
				user.getUsername(),
				user.getDisplayName(),
				user.getEmail(),
				user.getFirstName(),
				user.getLastName(),
				user.getBio(),
				user.getAvatarUrl(),
				user.getTimezone(),
				user.getRole(),
				user.isEmailVerified(),
				user.isTwoFactorEnabled(),
				user.getLastSeen(),
				user.getCreatedAt()
		);
	}
}
