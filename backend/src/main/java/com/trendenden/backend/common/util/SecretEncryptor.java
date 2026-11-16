package com.trendenden.backend.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * AES-256-GCM for the one secret the database has to be able to give back:
 * {@code users.two_factor_secret}.
 *
 * <p>A TOTP secret cannot be hashed. Verifying a code means recomputing it, which needs
 * the original bytes, so the only protection available is encryption with a key that is
 * not in the database. If the secret were stored in the clear, one dump would let an
 * attacker generate valid codes for every account and the second factor would be worth
 * nothing.
 *
 * <p>GCM rather than CBC: it authenticates as well as encrypts, so a tampered ciphertext
 * fails loudly instead of decrypting to garbage.
 */
@Component
public class SecretEncryptor {

	/** 96 bits is the size GCM is specified and optimised for. */
	private static final int IV_BYTES = 12;
	private static final int TAG_BITS = 128;
	private static final int KEY_BYTES = 32;
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";

	private static final SecureRandom RANDOM = new SecureRandom();

	private final SecretKey key;

	public SecretEncryptor(@Value("${app.security.encryption-key}") String base64Key) {
		byte[] raw;
		try {
			raw = Base64.getDecoder().decode(base64Key.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalStateException(
					"app.security.encryption-key must be base64-encoded", ex);
		}
		// Refusing at startup beats discovering a short key when the first user enrols.
		if (raw.length != KEY_BYTES) {
			throw new IllegalStateException("app.security.encryption-key must decode to "
					+ KEY_BYTES + " bytes for AES-256, got " + raw.length);
		}
		this.key = new SecretKeySpec(raw, "AES");
	}

	/** @return base64 of {@code iv || ciphertext || tag}, safe for a VARCHAR column */
	public String encrypt(String plaintext) {
		try {
			// A fresh random IV per encryption. Reusing one with the same key destroys
			// GCM's guarantees completely, so it must never be a constant.
			byte[] iv = new byte[IV_BYTES];
			RANDOM.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

			// The IV is not secret, only non-repeating, so it travels with the ciphertext.
			byte[] combined = new byte[iv.length + ciphertext.length];
			System.arraycopy(iv, 0, combined, 0, iv.length);
			System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
			return Base64.getEncoder().encodeToString(combined);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("Failed to encrypt secret", ex);
		}
	}

	public String decrypt(String stored) {
		try {
			byte[] combined = Base64.getDecoder().decode(stored);
			if (combined.length <= IV_BYTES) {
				throw new IllegalStateException("Stored secret is too short to be valid");
			}
			byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
			byte[] ciphertext = Arrays.copyOfRange(combined, IV_BYTES, combined.length);

			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		}
		catch (GeneralSecurityException ex) {
			// Also the path for a tampered ciphertext: GCM's tag check fails here.
			throw new IllegalStateException("Failed to decrypt secret", ex);
		}
	}
}
