package com.trendenden.backend.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.trendenden.backend.auth.service.TotpService;
import com.trendenden.backend.common.util.Base32;
import com.trendenden.backend.common.util.SecretEncryptor;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class TotpServiceTest {

	private static final String KEY = Base64.getEncoder()
			.encodeToString("0123456789abcdef0123456789abcdef".getBytes());

	private final TotpService totpService =
			new TotpService(new SecretEncryptor(KEY), "SkillSwap");

	/** 20 bytes is the RFC 4226 recommendation, which is 32 Base32 characters. */
	@Test
	void generatesATwentyByteBase32Secret() {
		String secret = totpService.generateSecret();

		assertThat(secret).hasSize(32).matches("[A-Z2-7]+");
		assertThat(Base32.decode(secret)).hasSize(20);
	}

	@Test
	void secretsDoNotRepeat() {
		assertThat(totpService.generateSecret()).isNotEqualTo(totpService.generateSecret());
	}

	@Test
	void acceptsTheCodeAnAuthenticatorAppWouldShowRightNow() {
		String secret = totpService.generateSecret();

		assertThat(totpService.verifyPlain(secret, codeAt(secret, Instant.now()))).isTrue();
	}

	@Test
	void rejectsAWrongCode() {
		String secret = totpService.generateSecret();
		String correct = codeAt(secret, Instant.now());
		String wrong = correct.equals("000000") ? "111111" : "000000";

		assertThat(totpService.verifyPlain(secret, wrong)).isFalse();
	}

	/**
	 * One step of tolerance each way. Without it, a phone clock a few seconds out, or a
	 * user typing as the code rolls over, locks the account out.
	 */
	@Test
	void acceptsOneStepOfClockDriftInBothDirections() {
		String secret = totpService.generateSecret();

		assertThat(totpService.verifyPlain(secret,
				codeAt(secret, Instant.now().minusSeconds(30)))).isTrue();
		assertThat(totpService.verifyPlain(secret,
				codeAt(secret, Instant.now().plusSeconds(30)))).isTrue();
	}

	/** The window is bounded: an old code must not stay usable. */
	@Test
	void rejectsACodeFromWellOutsideTheWindow() {
		String secret = totpService.generateSecret();

		assertThat(totpService.verifyPlain(secret,
				codeAt(secret, Instant.now().minusSeconds(300)))).isFalse();
	}

	@Test
	void verifiesAgainstAnEncryptedStoredSecret() {
		String secret = totpService.generateSecret();
		String stored = totpService.encrypt(secret);

		assertThat(stored).isNotEqualTo(secret);
		assertThat(totpService.verify(stored, codeAt(secret, Instant.now()))).isTrue();
		assertThat(totpService.verify(stored, "000001")).isFalse();
	}

	@Test
	void nullAndNonNumericInputAreRejectedWithoutThrowing() {
		String stored = totpService.encrypt(totpService.generateSecret());

		assertThat(totpService.verify(null, "123456")).isFalse();
		assertThat(totpService.verify(stored, null)).isFalse();
		assertThat(totpService.verify(stored, "abcdef")).isFalse();
	}

	/** The parameters an authenticator app needs; a wrong period or digits breaks scanning. */
	@Test
	void otpAuthUriCarriesEverythingAnAppNeeds() {
		String secret = "JBSWY3DPEHPK3PXP";

		String uri = totpService.buildOtpAuthUri(secret, "risattou@example.com");

		assertThat(uri)
				.startsWith("otpauth://totp/SkillSwap:")
				.contains("secret=" + secret)
				.contains("issuer=SkillSwap")
				.contains("algorithm=SHA1")
				.contains("digits=6")
				.contains("period=30");
		// The label is URL-encoded, so the "@" must not appear raw.
		assertThat(uri).contains("risattou%40example.com");
	}

	/** Generated with the library directly, so the test does not reuse the code under test. */
	private String codeAt(String base32Secret, Instant at) {
		try {
			var generator = new TimeBasedOneTimePasswordGenerator(
					Duration.ofSeconds(30), 6,
					TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
			return String.format("%06d", generator.generateOneTimePassword(
					new SecretKeySpec(Base32.decode(base32Secret), "HmacSHA1"), at));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
