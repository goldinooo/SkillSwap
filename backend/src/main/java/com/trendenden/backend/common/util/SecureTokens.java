package com.trendenden.backend.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Generates and hashes bearer secrets: refresh tokens and API keys.
 *
 * <p>Both are stored as a SHA-256 hex digest, never in the clear, so a database leak
 * cannot be replayed against the API.
 *
 * <p>SHA-256 rather than BCrypt is deliberate. These values are 256 bits of output from
 * {@link SecureRandom}, so there is no dictionary to attack and nothing for key
 * stretching to slow down. They are also verified on every single request, where BCrypt's
 * ~100ms would dominate the response time. A human-chosen password is the opposite case
 * on both counts, which is why it gets BCrypt instead.
 */
public final class SecureTokens {

	/** 32 bytes = 256 bits, matching the strength of the SHA-256 digest that stores it. */
	private static final int TOKEN_BYTES = 32;

	private static final SecureRandom RANDOM = new SecureRandom();

	private SecureTokens() {
	}

	/** URL-safe and unpadded, so the value is safe in a header, a URL or a JSON body. */
	public static String randomToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** Lowercase hex, always 64 characters: exactly what V10 sizes its hash columns for. */
	public static String sha256Hex(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(
					digest.digest(value.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			// SHA-256 is required of every JVM, so this cannot happen.
			throw new IllegalStateException("SHA-256 not available", ex);
		}
	}
}
