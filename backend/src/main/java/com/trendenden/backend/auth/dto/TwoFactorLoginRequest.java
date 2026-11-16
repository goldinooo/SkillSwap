package com.trendenden.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Completes a login that stopped at the 2FA step.
 *
 * @param challengeToken the interim token the password step returned
 * @param code           the six digits from the authenticator app
 */
public record TwoFactorLoginRequest(
		@NotBlank String challengeToken,
		@NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be six digits") String code
) {
}
