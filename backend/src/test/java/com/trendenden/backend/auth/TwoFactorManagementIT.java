package com.trendenden.backend.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.trendenden.backend.auth.repository.RefreshTokenRepository;
import com.trendenden.backend.auth.service.RefreshTokenService;
import com.trendenden.backend.auth.service.TokenService;
import com.trendenden.backend.common.IntegrationTest;
import com.trendenden.backend.common.util.Base32;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import com.trendenden.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class TwoFactorManagementIT {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	MockMvc mvc;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	TokenService tokenService;

	@Autowired
	RefreshTokenService refreshTokenService;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@Autowired
	ObjectMapper objectMapper;

	/**
	 * Enrolment hands out a secret but leaves 2FA off. Switching it on here would lock out
	 * anyone whose scan silently failed, since they would have no way to produce a code.
	 */
	@Test
	void enrolmentStagesTheSecretWithoutTurningTwoFactorOn() throws Exception {
		User user = register("enrol", "enrol@example.com");

		String body = mvc.perform(post("/api/account/2fa/enroll")
						.header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.secret").exists())
				.andExpect(jsonPath("$.otpAuthUri").exists())
				.andReturn().getResponse().getContentAsString();

		assertThat(objectMapper.readTree(body).get("otpAuthUri").asString())
				.startsWith("otpauth://totp/SkillSwap:");

		User reloaded = userRepository.findById(user.getId()).orElseThrow();
		assertThat(reloaded.isTwoFactorEnabled()).isFalse();
		// Staged, and stored as ciphertext rather than the Base32 value.
		assertThat(reloaded.getTwoFactorSecret()).isNotNull()
				.isNotEqualTo(objectMapper.readTree(body).get("secret").asString());
	}

	@Test
	void aValidCodeCompletesEnrolmentAndEndsOtherSessions() throws Exception {
		User user = register("confirm", "confirm@example.com");
		// A session that predates 2FA, so it was never checked against a second factor.
		refreshTokenService.issue(user);
		assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
				.hasSize(1);

		String secret = enrol(user);

		mvc.perform(post("/api/account/2fa/confirm")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"secret":"%s","code":"%s"}"""
								.formatted(secret, codeFor(secret))))
				.andExpect(status().isNoContent());

		assertThat(userRepository.findById(user.getId()).orElseThrow().isTwoFactorEnabled())
				.isTrue();
		// Otherwise enabling 2FA would only protect future logins.
		assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
				.isEmpty();
	}

	@Test
	void aWrongCodeDoesNotCompleteEnrolment() throws Exception {
		User user = register("badconfirm", "badconfirm@example.com");
		String secret = enrol(user);
		String wrong = codeFor(secret).equals("000000") ? "111111" : "000000";

		mvc.perform(post("/api/account/2fa/confirm")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"secret":"%s","code":"%s"}""".formatted(secret, wrong)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"));

		assertThat(userRepository.findById(user.getId()).orElseThrow().isTwoFactorEnabled())
				.isFalse();
	}

	@Test
	void enrollingTwiceIsRejected() throws Exception {
		User user = register("double", "double@example.com");
		String secret = enrol(user);
		mvc.perform(post("/api/account/2fa/confirm")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"secret":"%s","code":"%s"}"""
								.formatted(secret, codeFor(secret))))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/account/2fa/enroll").header("Authorization", bearer(user)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TWO_FACTOR_ALREADY_ENABLED"));
	}

	/**
	 * Turning the second factor off re-checks the password. If an access token were
	 * stolen, disabling 2FA would otherwise be the attacker's first and easiest move.
	 */
	@Test
	void disablingNeedsThePasswordAgain() throws Exception {
		User user = register("disable", "disable@example.com");
		String secret = enrol(user);
		mvc.perform(post("/api/account/2fa/confirm")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"secret":"%s","code":"%s"}"""
								.formatted(secret, codeFor(secret))))
				.andExpect(status().isNoContent());

		mvc.perform(delete("/api/account/2fa")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"password":"not-the-password"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));

		assertThat(userRepository.findById(user.getId()).orElseThrow().isTwoFactorEnabled())
				.isTrue();

		mvc.perform(delete("/api/account/2fa")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"password":"%s"}""".formatted(PASSWORD)))
				.andExpect(status().isNoContent());

		User reloaded = userRepository.findById(user.getId()).orElseThrow();
		assertThat(reloaded.isTwoFactorEnabled()).isFalse();
		// Cleared together, so the state can never be flag-on with no secret.
		assertThat(reloaded.getTwoFactorSecret()).isNull();
	}

	/**
	 * These three live under /api/account, not /api/auth, because SecurityConfig permits
	 * all of /api/auth/** without a token.
	 */
	@Test
	void twoFactorManagementIsNotReachableWithoutAToken() throws Exception {
		mvc.perform(post("/api/account/2fa/enroll"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private User register(String username, String email) {
		return userService.register(new RegisterRequest(username, email, PASSWORD, null));
	}

	private String enrol(User user) throws Exception {
		String body = mvc.perform(post("/api/account/2fa/enroll")
						.header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("secret").asString();
	}

	private String bearer(User user) {
		return "Bearer " + tokenService.issueAccessToken(user).token();
	}

	private String codeFor(String base32Secret) {
		try {
			var generator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30), 6,
					TimeBasedOneTimePasswordGenerator.TOTP_ALGORITHM_HMAC_SHA1);
			return String.format("%06d", generator.generateOneTimePassword(
					new SecretKeySpec(Base32.decode(base32Secret), "HmacSHA1"), Instant.now()));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}
	}
}
