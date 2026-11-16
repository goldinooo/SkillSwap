package com.trendenden.backend.common.util;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SecureTokensTest {

	/** V10 sizes key_hash as VARCHAR(64), so anything else would not fit. */
	@Test
	void sha256HexIsAlwaysSixtyFourLowercaseHexCharacters() {
		assertThat(SecureTokens.sha256Hex("anything"))
				.hasSize(64)
				.matches("[0-9a-f]{64}");
		assertThat(SecureTokens.sha256Hex("")).hasSize(64);
	}

	/** Pinned against a known digest, so a change of algorithm cannot pass unnoticed. */
	@Test
	void sha256HexMatchesTheKnownDigestOfAbc() {
		assertThat(SecureTokens.sha256Hex("abc")).isEqualTo(
				"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
	}

	@Test
	void sameInputAlwaysHashesToTheSameValue() {
		assertThat(SecureTokens.sha256Hex("ssk_abc"))
				.isEqualTo(SecureTokens.sha256Hex("ssk_abc"));
	}

	/** A collision here would mean one user's token authenticating as another's. */
	@Test
	void tokensDoNotRepeat() {
		Set<String> seen = new HashSet<>();
		for (int i = 0; i < 2_000; i++) {
			assertThat(seen.add(SecureTokens.randomToken()))
					.as("token %d was a duplicate", i).isTrue();
		}
	}

	/** URL-safe and unpadded, so it is usable in a header, a query string or JSON. */
	@Test
	void tokensAreUrlSafeBase64WithoutPadding() {
		for (int i = 0; i < 100; i++) {
			assertThat(SecureTokens.randomToken())
					.matches("[A-Za-z0-9_-]+")
					.doesNotContain("=");
		}
	}
}
