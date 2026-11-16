package com.trendenden.backend.common.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Base32Test {

	/**
	 * The RFC 4648 section 10 vectors, minus padding. Round-tripping alone would pass even
	 * if the alphabet or bit order were wrong, so the encoding is pinned against the spec.
	 */
	@Test
	void matchesTheRfc4648TestVectors() {
		assertThat(encode("f")).isEqualTo("MY");
		assertThat(encode("fo")).isEqualTo("MZXQ");
		assertThat(encode("foo")).isEqualTo("MZXW6");
		assertThat(encode("foob")).isEqualTo("MZXW6YQ");
		assertThat(encode("fooba")).isEqualTo("MZXW6YTB");
		assertThat(encode("foobar")).isEqualTo("MZXW6YTBOI");
	}

	@Test
	void roundTripsEveryLengthThatMattersForTotp() {
		SecureRandom random = new SecureRandom();
		for (int length = 1; length <= 32; length++) {
			byte[] original = new byte[length];
			random.nextBytes(original);
			assertThat(Base32.decode(Base32.encode(original)))
					.as("round trip of %d bytes", length)
					.isEqualTo(original);
		}
	}

	/** Authenticator apps show the secret in groups and upper case; users retype it freely. */
	@Test
	void decodeAcceptsLowerCaseAndPadding() {
		assertThat(Base32.decode("mzxw6ytboi")).isEqualTo("foobar".getBytes(StandardCharsets.UTF_8));
		assertThat(Base32.decode("MZXW6YTBOI======"))
				.isEqualTo("foobar".getBytes(StandardCharsets.UTF_8));
	}

	/** 0, 1 and 8 are not in the alphabet precisely because they look like O, I and B. */
	@Test
	void decodeRejectsCharactersOutsideTheAlphabet() {
		assertThatThrownBy(() -> Base32.decode("MZXW6YTB01"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Not a Base32 character");
	}

	private String encode(String text) {
		return Base32.encode(text.getBytes(StandardCharsets.UTF_8));
	}
}
