package com.trendenden.backend.user.dto;

import jakarta.validation.constraints.Size;

/**
 * A full replacement of the editable profile fields: a null field clears it. Only
 * {@code displayName} is kept when null, because V10 forbids a null display name.
 */
public record UpdateProfileRequest(

		@Size(min = 1, max = 255)
		String displayName,

		@Size(max = 50)
		String firstName,

		@Size(max = 50)
		String lastName,

		@Size(max = 2000)
		String bio,

		@Size(max = 255)
		String avatarUrl,

		/** IANA zone id, e.g. "Africa/Casablanca". Validated against the JDK's zone list. */
		@Size(max = 50)
		String timezone
) {
}
