package com.trendenden.backend.auth.service;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.trendenden.backend.common.util.Base32;
import com.trendenden.backend.common.util.SecretEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;

/**
 * TOTP enrolment and verification on top of {@code java-otp}.
 *
 * <p>{@code java-otp} is only a code generator: it has no notion of enrolment, QR codes,
 * storage or clock tolerance. Everything around the six digits is here.
 */
@Service
public class TotpService {

	private static final Logger log = LoggerFactory.getLogger(TotpService.class);

	/** 160 bits, the size RFC 4226 recommends for an HMAC-SHA1 TOTP secret. */
	private static final int SECRET_BYTES = 20;

	private static final String HMAC_ALGORITHM = "HmacSHA1";

	/**
	 * How many 30-second steps either side of now are accepted.
	 *
	 * <p>Without any tolerance, a phone clock a few seconds off its true time locks the
	 * user out. One step each way covers that and typing delay, and costs an attacker only
	 * a factor of three on a 10^6 search space that is already rate-limited.
	 */
	private static final int ALLOWED_DRIFT_STEPS = 1;

	private static final SecureRandom RANDOM = new SecureRandom();

	private final TimeBasedOneTimePasswordGenerator generator;
	private final SecretEncryptor secretEncryptor;
	private final String issuer;

	public TotpService(SecretEncryptor secretEncryptor,
			@org.springframework.beans.factory.annotation.Value("${app.totp.issuer}")
			String issuer) {
		this.secretEncryptor = secretEncryptor;
		this.issuer = issuer;
		try {
			// SHA-1 is not a free choice here: Google Authenticator, Authy and the rest
			// assume SHA-1 with a 30-second step and six digits. SHA-256 would be
			// stronger in the abstract and unusable in practice.
			this.generator = new TimeBasedOneTimePasswordGenerator(
					Duration.ofSeconds(30), 6,
					TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("HmacSHA1 not available", ex);
		}
	}

	/** A new Base32 secret, in the clear. The caller encrypts it before storing. */
	public String generateSecret() {
		byte[] bytes = new byte[SECRET_BYTES];
		RANDOM.nextBytes(bytes);
		return Base32.encode(bytes);
	}

	public String encrypt(String base32Secret) {
		return secretEncryptor.encrypt(base32Secret);
	}

	/**
	 * The URI an authenticator app expects, which the frontend renders as a QR code.
	 *
	 * <p>Built rather than drawn: turning this into a PNG server-side would need an
	 * imaging dependency for no benefit, since the client can render it and the secret
	 * then never appears in an image cache.
	 */
	public String buildOtpAuthUri(String base32Secret, String accountName) {
		String label = encode(issuer) + ":" + encode(accountName);
		return "otpauth://totp/" + label
				+ "?secret=" + base32Secret
				+ "&issuer=" + encode(issuer)
				+ "&algorithm=SHA1&digits=6&period=30";
	}

	/**
	 * @param encryptedSecret the value from {@code users.two_factor_secret}
	 * @param code            the six digits the user typed
	 * @return whether the code is valid now, or within the accepted drift
	 */
	public boolean verify(String encryptedSecret, String code) {
		if (encryptedSecret == null || code == null) {
			return false;
		}
		int expected;
		try {
			expected = Integer.parseInt(code.trim());
		}
		catch (NumberFormatException ex) {
			return false;
		}
		return verifyDecrypted(secretEncryptor.decrypt(encryptedSecret), expected);
	}

	/** Same check against a secret that is not stored yet, used during enrolment. */
	public boolean verifyPlain(String base32Secret, String code) {
		try {
			return verifyDecrypted(base32Secret, Integer.parseInt(code.trim()));
		}
		catch (NumberFormatException ex) {
			return false;
		}
	}

	private boolean verifyDecrypted(String base32Secret, int expected) {
		SecretKey key = new SecretKeySpec(Base32.decode(base32Secret), HMAC_ALGORITHM);
		Instant now = Instant.now();
		try {
			for (int step = -ALLOWED_DRIFT_STEPS; step <= ALLOWED_DRIFT_STEPS; step++) {
				Instant at = now.plus(generator.getTimeStep().multipliedBy(step));
				if (generator.generateOneTimePassword(key, at) == expected) {
					return true;
				}
			}
			return false;
		}
		catch (InvalidKeyException ex) {
			log.warn("Stored TOTP secret could not be used as an HMAC key", ex);
			return false;
		}
	}

	private String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}
}
