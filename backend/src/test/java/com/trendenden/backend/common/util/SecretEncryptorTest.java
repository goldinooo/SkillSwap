package com.trendenden.backend.common.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretEncryptorTest {

	private static final String KEY = Base64.getEncoder()
			.encodeToString("0123456789abcdef0123456789abcdef".getBytes());

	private final SecretEncryptor encryptor = new SecretEncryptor(KEY);

	@Test
	void roundTripsASecret() {
		String secret = "JBSWY3DPEHPK3PXP";
		assertThat(encryptor.decrypt(encryptor.encrypt(secret))).isEqualTo(secret);
	}

	/**
	 * The random IV is what makes this true, and it matters: identical ciphertexts would
	 * tell anyone reading the table which accounts share a secret.
	 */
	@Test
	void encryptingTheSameValueTwiceGivesDifferentCiphertext() {
		String first = encryptor.encrypt("same-secret");
		String second = encryptor.encrypt("same-secret");

		assertThat(first).isNotEqualTo(second);
		assertThat(encryptor.decrypt(first)).isEqualTo("same-secret");
		assertThat(encryptor.decrypt(second)).isEqualTo("same-secret");
	}

	/**
	 * GCM authenticates as well as encrypts. This is the difference from CBC, where a
	 * tampered ciphertext decrypts to garbage and is used as if it were real.
	 */
	@Test
	void tamperedCiphertextIsRejectedRatherThanDecryptedToGarbage() {
		byte[] raw = Base64.getDecoder().decode(encryptor.encrypt("JBSWY3DPEHPK3PXP"));
		raw[raw.length - 1] ^= 0x01;
		String tampered = Base64.getEncoder().encodeToString(raw);

		assertThatThrownBy(() -> encryptor.decrypt(tampered))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to decrypt");
	}

	@Test
	void aTruncatedValueIsRejected() {
		assertThatThrownBy(() -> encryptor.decrypt(
				Base64.getEncoder().encodeToString(new byte[8])))
				.isInstanceOf(IllegalStateException.class);
	}

	/** Checked in the constructor so a bad key stops startup, not the first enrolment. */
	@Test
	void aKeyOfTheWrongLengthIsRejectedAtConstruction() {
		String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

		assertThatThrownBy(() -> new SecretEncryptor(shortKey))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("32 bytes");
	}

	@Test
	void aKeyThatIsNotBase64IsRejectedAtConstruction() {
		assertThatThrownBy(() -> new SecretEncryptor("not base64 !!!"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("base64");
	}
}
