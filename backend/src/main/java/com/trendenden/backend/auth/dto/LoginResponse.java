package com.trendenden.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * One response shape for both outcomes of a correct password, so the client has a single
 * payload to parse and branches on {@code twoFactorRequired}.
 *
 * <p>{@code @JsonInclude(NON_NULL)} keeps the unused half out of the body: a completed
 * login carries no {@code challengeToken}, and a 2FA challenge carries no tokens.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResponse(
		boolean twoFactorRequired,
		String challengeToken,
		String accessToken,
		String refreshToken,
		Instant accessTokenExpiresAt
) {

	public static LoginResponse completed(String accessToken, Instant expiresAt,
			String refreshToken) {
		return new LoginResponse(false, null, accessToken, refreshToken, expiresAt);
	}

	public static LoginResponse twoFactorRequired(String challengeToken) {
		return new LoginResponse(true, challengeToken, null, null, null);
	}
}
