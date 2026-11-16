package com.trendenden.backend.auth;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.trendenden.backend.auth.repository.RefreshTokenRepository;
import com.trendenden.backend.auth.service.TotpService;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class AuthFlowIT {

	private static final String PASSWORD = "correct-horse-battery";

	@Autowired
	MockMvc mvc;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	RefreshTokenRepository refreshTokenRepository;

	@Autowired
	TotpService totpService;

	@Autowired
	ObjectMapper objectMapper;

	// ---------------------------------------------------------------- login

	@Test
	void logsInAndTheAccessTokenWorksOnAProtectedEndpoint() throws Exception {
		register("loginok", "loginok@example.com");

		JsonNode tokens = login("loginok@example.com", PASSWORD);

		assertThat(tokens.get("twoFactorRequired").asBoolean()).isFalse();
		assertThat(tokens.get("accessToken").asString()).isNotBlank();
		assertThat(tokens.get("refreshToken").asString()).isNotBlank();

		mvc.perform(get("/api/users/me")
						.header("Authorization", "Bearer " + tokens.get("accessToken").asString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("loginok"));
	}

	@Test
	void theEmailIsMatchedCaseInsensitively() throws Exception {
		register("caseuser", "caseuser@example.com");

		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
						{"email":"CaseUser@Example.COM","password":"%s"}""".formatted(PASSWORD)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists());
	}

	/**
	 * The three ways a login can fail must be indistinguishable. If they were not, an
	 * attacker could test an address list against the platform and learn who is registered.
	 */
	@Test
	void unknownEmailWrongPasswordAndProviderOnlyAccountAllAnswerIdentically() throws Exception {
		register("realuser", "realuser@example.com");
		// An account created through Google or 42: no password hash at all.
		userRepository.save(User.createFromProvider("oauthonly", "oauthonly@example.com", "O"));

		String unknown = failedLogin("nobody@example.com", PASSWORD);
		String wrongPassword = failedLogin("realuser@example.com", "not-the-password");
		String providerOnly = failedLogin("oauthonly@example.com", PASSWORD);

		assertThat(unknown).isEqualTo(wrongPassword).isEqualTo(providerOnly);
		assertThat(unknown).contains("INVALID_CREDENTIALS");
	}

	@Test
	void aGarbageTokenIsRejectedWithoutAnError() throws Exception {
		mvc.perform(get("/api/users/me").header("Authorization", "Bearer not.a.jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	/** Signed with a different key, so this proves the signature is actually verified. */
	@Test
	void aTokenSignedWithAnotherKeyIsRejected() throws Exception {
		String forged = "eyJhbGciOiJIUzI1NiJ9."
				+ "eyJzdWIiOiJmOTMyNGE2Yi04YmRhLTRlZGItYjU3MC1mZTYzNjhhOWU3NzgiLCJ1c2VybmFtZSI6"
				+ "ImhhY2tlciIsInJvbGUiOiJBRE1JTiIsImlzcyI6InNraWxsc3dhcCIsImV4cCI6NDA3MDkwODgwMH0."
				+ "aGFja2VyLXNpZ25hdHVyZS10aGF0LWRvZXMtbm90LXZlcmlmeQ";

		mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + forged))
				.andExpect(status().isUnauthorized());
	}

	// -------------------------------------------------------------- refresh

	@Test
	void refreshReturnsANewPairAndRetiresTheOldRefreshToken() throws Exception {
		register("rotate", "rotate@example.com");
		JsonNode first = login("rotate@example.com", PASSWORD);
		String firstRefresh = first.get("refreshToken").asString();

		JsonNode second = refresh(firstRefresh);
		String secondRefresh = second.get("refreshToken").asString();

		assertThat(secondRefresh).isNotEqualTo(firstRefresh);
		assertThat(second.get("accessToken").asString()).isNotBlank();

		// The replacement works on a real endpoint.
		mvc.perform(get("/api/users/me")
						.header("Authorization", "Bearer " + second.get("accessToken").asString()))
				.andExpect(status().isOk());
	}

	/**
	 * Rotation is what makes theft detectable. A retired token presented again means two
	 * copies exist, so every session of that user is ended rather than guessing which
	 * caller is the legitimate one.
	 */
	@Test
	void reusingARetiredRefreshTokenRevokesEverySessionOfThatUser() throws Exception {
		User user = register("reuse", "reuse@example.com");
		String firstRefresh = login("reuse@example.com", PASSWORD).get("refreshToken").asString();
		String secondRefresh = refresh(firstRefresh).get("refreshToken").asString();

		// The attacker replays the stolen, already-spent token.
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}""".formatted(firstRefresh)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("REFRESH_TOKEN_REUSED"));

		// And the legitimate client's current token is dead too: the family is burned.
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}""".formatted(secondRefresh)))
				.andExpect(status().isUnauthorized());

		assertThat(refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId()))
				.isEmpty();
	}

	@Test
	void anUnknownRefreshTokenIsRejected() throws Exception {
		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"never-issued"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
	}

	/** This is what an access JWT cannot do, and the reason refresh tokens are stored. */
	@Test
	void logoutMakesTheRefreshTokenUnusable() throws Exception {
		register("logout", "logout@example.com");
		String refreshToken = login("logout@example.com", PASSWORD).get("refreshToken").asString();

		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}""".formatted(refreshToken)))
				.andExpect(status().isNoContent());

		mvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}""".formatted(refreshToken)))
				.andExpect(status().isUnauthorized());
	}

	/** Logout is idempotent: the client's goal is reached either way. */
	@Test
	void loggingOutAnUnknownTokenStillSucceeds() throws Exception {
		mvc.perform(post("/api/auth/logout").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"never-issued"}"""))
				.andExpect(status().isNoContent());
	}

	// ------------------------------------------------------------------ 2FA

	@Test
	void anAccountWithTwoFactorGetsAChallengeInsteadOfTokens() throws Exception {
		User user = register("twofa", "twofa@example.com");
		enableTwoFactor(user);

		JsonNode response = login("twofa@example.com", PASSWORD);

		assertThat(response.get("twoFactorRequired").asBoolean()).isTrue();
		assertThat(response.get("challengeToken").asString()).isNotBlank();
		// No session has been handed out yet.
		assertThat(response.has("accessToken")).isFalse();
		assertThat(response.has("refreshToken")).isFalse();
	}

	/**
	 * The challenge is signed with the same key as an access token, so without the scope
	 * check in {@code TokenService.parse} a correct password alone would authenticate and
	 * the second factor would be bypassable.
	 */
	@Test
	void theChallengeTokenCannotBeUsedAsAnAccessToken() throws Exception {
		User user = register("bypass", "bypass@example.com");
		enableTwoFactor(user);
		String challenge = login("bypass@example.com", PASSWORD).get("challengeToken").asString();

		mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + challenge))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void aValidCodeCompletesTheLogin() throws Exception {
		User user = register("twofaok", "twofaok@example.com");
		String secret = enableTwoFactor(user);
		String challenge = login("twofaok@example.com", PASSWORD).get("challengeToken").asString();

		String body = mvc.perform(post("/api/auth/2fa").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"challengeToken":"%s","code":"%s"}"""
								.formatted(challenge, codeFor(secret))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").exists())
				.andReturn().getResponse().getContentAsString();

		mvc.perform(get("/api/users/me").header("Authorization",
						"Bearer " + objectMapper.readTree(body).get("accessToken").asString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.twoFactorEnabled").value(true));
	}

	@Test
	void aWrongCodeDoesNotCompleteTheLogin() throws Exception {
		User user = register("twofabad", "twofabad@example.com");
		String secret = enableTwoFactor(user);
		String challenge = login("twofabad@example.com", PASSWORD).get("challengeToken").asString();
		String wrong = codeFor(secret).equals("000000") ? "111111" : "000000";

		mvc.perform(post("/api/auth/2fa").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"challengeToken":"%s","code":"%s"}""".formatted(challenge, wrong)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOTP_CODE"));
	}

	@Test
	void aFabricatedChallengeTokenIsRejected() throws Exception {
		mvc.perform(post("/api/auth/2fa").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"challengeToken":"made.up.token","code":"123456"}"""))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CHALLENGE"));
	}

	@Test
	void aCodeThatIsNotSixDigitsIsRejectedByValidation() throws Exception {
		mvc.perform(post("/api/auth/2fa").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"challengeToken":"anything","code":"12ab"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	// -------------------------------------------------------------- helpers

	private User register(String username, String email) {
		return userService.register(new RegisterRequest(username, email, PASSWORD, null));
	}

	private String enableTwoFactor(User user) {
		String secret = totpService.generateSecret();
		user.enableTwoFactor(totpService.encrypt(secret));
		userRepository.saveAndFlush(user);
		return secret;
	}

	private JsonNode login(String email, String password) throws Exception {
		String body = mvc.perform(post("/api/auth/login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}""".formatted(email, password)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body);
	}

	private String failedLogin(String email, String password) throws Exception {
		return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"email":"%s","password":"%s"}""".formatted(email, password)))
				.andExpect(status().isUnauthorized())
				.andReturn().getResponse().getContentAsString()
				// The timestamp differs between calls; everything else must not.
				.replaceAll("\"timestamp\":\"[^\"]+\"", "");
	}

	private JsonNode refresh(String refreshToken) throws Exception {
		String body = mvc.perform(post("/api/auth/refresh")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"refreshToken":"%s"}""".formatted(refreshToken)))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body);
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
