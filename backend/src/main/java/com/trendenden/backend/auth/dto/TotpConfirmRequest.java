package com.trendenden.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Confirms enrolment by proving the authenticator app really holds the staged secret.
 *
 * @param secret the value returned by the enrol call
 * @param code   six digits generated from it
 */
public record TotpConfirmRequest(
		@NotBlank String secret,
		@NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "must be six digits") String code
) {
}
