package com.trendenden.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Validated by {@code @Valid} in the controller; failures are turned into one 400 listing
 * every bad field by {@code GlobalExceptionHandler}.
 */
public record RegisterRequest(

		// The pattern accepts upper case because the service lowercases afterwards; it is
		// the stored value that must be lowercase, not what the user typed.
		@NotBlank
		@Size(min = 3, max = 50)
		@Pattern(regexp = "^[A-Za-z0-9._-]+$",
				message = "may only contain letters, digits, dot, underscore and hyphen")
		String username,

		@NotBlank
		@Email
		@Size(max = 255)
		String email,

		// 72 is not arbitrary: BCrypt hashes only the first 72 bytes and silently ignores
		// the rest, so a longer password would give a false sense of strength.
		@NotBlank
		@Size(min = 8, max = 72)
		String password,

		/** Optional. V10 makes display_name NOT NULL, so the service falls back to the username. */
		@Size(max = 255)
		String displayName
) {
}
