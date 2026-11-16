package com.trendenden.backend.auth.entity;

import java.util.Optional;

/**
 * Providers the platform accepts, matching the CHECK constraint on
 * {@code oauth.provider} in V10.
 *
 * <p>{@code FORTYTWO} rather than {@code 42} because a Java enum constant cannot start
 * with a digit, and the stored value has to match the constant name exactly.
 */
public enum OAuthProvider {

	GOOGLE("google"),
	FORTYTWO("fortytwo");

	/** The {@code registrationId} in {@code spring.security.oauth2.client.registration.*}. */
	private final String registrationId;

	OAuthProvider(String registrationId) {
		this.registrationId = registrationId;
	}

	public String registrationId() {
		return registrationId;
	}

	public static Optional<OAuthProvider> fromRegistrationId(String registrationId) {
		for (OAuthProvider provider : values()) {
			if (provider.registrationId.equalsIgnoreCase(registrationId)) {
				return Optional.of(provider);
			}
		}
		return Optional.empty();
	}
}
