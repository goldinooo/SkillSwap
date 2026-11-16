package com.trendenden.backend.publicapi;

import com.trendenden.backend.auth.service.TokenService;
import com.trendenden.backend.common.IntegrationTest;
import com.trendenden.backend.publicapi.dto.CreateApiKeyRequest;
import com.trendenden.backend.publicapi.entity.ApiKey;
import com.trendenden.backend.publicapi.repository.ApiKeyRepository;
import com.trendenden.backend.publicapi.service.ApiKeyService;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class PublicApiIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserService userService;

	@Autowired
	ApiKeyService apiKeyService;

	@Autowired
	ApiKeyRepository apiKeyRepository;

	@Autowired
	TokenService tokenService;

	// ------------------------------------------------------------ key management

	@Test
	void createsAKeyAndReturnsItExactlyOnce() throws Exception {
		User user = register("keyowner", "keyowner@example.com");

		String body = mvc.perform(post("/api/account/api-keys")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"CI pipeline"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("CI pipeline"))
				.andExpect(jsonPath("$.key").exists())
				.andReturn().getResponse().getContentAsString();

		// The recognisable prefix is what lets a secret scanner spot a leaked key.
		assertThat(body).contains("\"key\":\"ssk_");

		// Listing it again must not include the key, nor its hash.
		mvc.perform(get("/api/account/api-keys").header("Authorization", bearer(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].key").doesNotExist())
				.andExpect(jsonPath("$[0].keyHash").doesNotExist())
				.andExpect(jsonPath("$[0].revoked").value(false));
	}

	/** Only the digest reaches the table, so a dump cannot be replayed. */
	@Test
	void onlyTheHashIsPersisted() throws Exception {
		User user = register("hashonly", "hashonly@example.com");

		String raw = apiKeyService.create(user.getId(), new CreateApiKeyRequest("k")).key();

		ApiKey stored = apiKeyRepository.findAll().stream()
				.filter(key -> key.getUser().getId().equals(user.getId()))
				.findFirst().orElseThrow();
		assertThat(stored.getKeyHash()).hasSize(64).isNotEqualTo(raw);
		assertThat(raw).isNotEmpty();
	}

	@Test
	void twoLiveKeysCannotShareAName() throws Exception {
		User user = register("dupname", "dupname@example.com");
		apiKeyService.create(user.getId(), new CreateApiKeyRequest("ci"));

		mvc.perform(post("/api/account/api-keys")
						.header("Authorization", bearer(user))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"ci"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("API_KEY_NAME_TAKEN"));
	}

	/** Reported as not found rather than forbidden, so key ids cannot be probed. */
	@Test
	void anotherUsersKeyCannotBeRevoked() throws Exception {
		User owner = register("owner", "owner@example.com");
		User stranger = register("stranger", "stranger@example.com");
		ApiKey key = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(hashOf(
				apiKeyService.create(owner.getId(), new CreateApiKeyRequest("k")).key()))
				.orElseThrow();

		mvc.perform(delete("/api/account/api-keys/{id}", key.getId())
						.header("Authorization", bearer(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("API_KEY_NOT_FOUND"));

		assertThat(apiKeyRepository.findById(key.getId()).orElseThrow().isRevoked()).isFalse();
	}

	@Test
	void keyManagementItselfNeedsAJwt() throws Exception {
		mvc.perform(get("/api/account/api-keys"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------------------------------------------------------- key authentication

	@Test
	void aValidKeyReachesThePublicApi() throws Exception {
		User user = register("pubuser", "pubuser@example.com");
		String key = newKey(user);

		mvc.perform(get("/api/v1/public/me").header("X-API-Key", key))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("pubuser"))
				// The public projection never carries an email.
				.andExpect(jsonPath("$.email").doesNotExist());
	}

	@Test
	void thePublicApiRefusesRequestsWithNoKey() throws Exception {
		mvc.perform(get("/api/v1/public/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void anUnknownKeyIsRefused() throws Exception {
		mvc.perform(get("/api/v1/public/me").header("X-API-Key", "ssk_not-a-real-key"))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * Revocation is a timestamp, not a delete. This is the test that would fail if the
	 * lookup were {@code findByKeyHash} instead of {@code findByKeyHashAndRevokedAtIsNull}.
	 */
	@Test
	void aRevokedKeyStopsWorking() throws Exception {
		User user = register("revoked", "revoked@example.com");
		String key = newKey(user);

		mvc.perform(get("/api/v1/public/me").header("X-API-Key", key))
				.andExpect(status().isOk());

		ApiKey stored = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(hashOf(key))
				.orElseThrow();
		apiKeyService.revoke(user.getId(), stored.getId());

		mvc.perform(get("/api/v1/public/me").header("X-API-Key", key))
				.andExpect(status().isUnauthorized());

		// The row survives, so an audit can still see the key existed.
		assertThat(apiKeyRepository.findById(stored.getId())).isPresent();
	}

	/** The two chains are separate on purpose: neither credential works on the other. */
	@Test
	void aJwtDoesNotAuthenticateAgainstThePublicApi() throws Exception {
		User user = register("jwtonly", "jwtonly@example.com");

		mvc.perform(get("/api/v1/public/me").header("Authorization", bearer(user)))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void anApiKeyDoesNotAuthenticateAgainstTheJwtApi() throws Exception {
		User user = register("keyonly", "keyonly@example.com");
		String key = newKey(user);

		mvc.perform(get("/api/users/me").header("X-API-Key", key))
				.andExpect(status().isUnauthorized());
	}

	// ------------------------------------------------------------------ endpoints

	@Test
	void healthIsReachableWithoutAKeySoClientsCanCheckReachability() throws Exception {
		mvc.perform(get("/api/v1/public/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"))
				.andExpect(jsonPath("$.version").value("v1"));
	}

	@Test
	void theDirectoryFiltersSortsAndPages() throws Exception {
		User caller = register("searcher", "searcher@example.com");
		register("alpha", "alpha@example.com");
		register("beta", "beta@example.com");
		register("gamma", "gamma@example.com");
		String key = newKey(caller);

		mvc.perform(get("/api/v1/public/users")
						.header("X-API-Key", key)
						.param("size", "2").param("page", "0")
						.param("sort", "username").param("direction", "asc"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(2))
				.andExpect(jsonPath("$.content[0].username").value("alpha"))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.first").value(true))
				.andExpect(jsonPath("$.last").value(false));

		mvc.perform(get("/api/v1/public/users")
						.header("X-API-Key", key).param("q", "gam"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content.length()").value(1))
				.andExpect(jsonPath("$.content[0].username").value("gamma"));
	}

	/**
	 * The sort parameter is allow-listed. Passed straight through it would reach the JPA
	 * metamodel, and ordering by {@code passwordHash} leaks information about values the
	 * caller must never see.
	 */
	@Test
	void anUnknownSortFieldFallsBackInsteadOfReachingTheMetamodel() throws Exception {
		User caller = register("sorter", "sorter@example.com");

		mvc.perform(get("/api/v1/public/users")
						.header("X-API-Key", newKey(caller))
						.param("sort", "passwordHash"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").isArray());
	}

	@Test
	void pageSizeIsCapped() throws Exception {
		User caller = register("bigpage", "bigpage@example.com");

		mvc.perform(get("/api/v1/public/users")
						.header("X-API-Key", newKey(caller)).param("size", "5000"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	void aSingleProfileIsAddressableByUsername() throws Exception {
		User caller = register("lookup", "lookup@example.com");
		register("target", "target@example.com");

		mvc.perform(get("/api/v1/public/users/{username}", "target")
						.header("X-API-Key", newKey(caller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.username").value("target"))
				.andExpect(jsonPath("$.email").doesNotExist());

		mvc.perform(get("/api/v1/public/users/{username}", "nobody")
						.header("X-API-Key", newKey(caller)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void statsReportAggregatesOnly() throws Exception {
		User caller = register("stats", "stats@example.com");

		mvc.perform(get("/api/v1/public/stats").header("X-API-Key", newKey(caller)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalUsers").isNumber())
				.andExpect(jsonPath("$.generatedAt").exists());
	}

	/** Lets a client pace itself before it runs out of quota. */
	@Test
	void rateLimitHeadersAreSentOnSuccess() throws Exception {
		User caller = register("headers", "headers@example.com");

		mvc.perform(get("/api/v1/public/stats").header("X-API-Key", newKey(caller)))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(result -> {
					assertThat(result.getResponse().getHeader("X-RateLimit-Limit")).isNotNull();
					assertThat(result.getResponse().getHeader("X-RateLimit-Remaining"))
							.isNotNull();
				});
	}

	// -------------------------------------------------------------------- helpers

	private User register(String username, String email) {
		return userService.register(
				new RegisterRequest(username, email, "correct-horse-battery", null));
	}

	private String newKey(User user) {
		return apiKeyService.create(user.getId(),
				new CreateApiKeyRequest("key-" + Instant.now().toEpochMilli()
						+ "-" + System.nanoTime())).key();
	}

	private String hashOf(String rawKey) {
		return com.trendenden.backend.common.util.SecureTokens.sha256Hex(rawKey);
	}

	private String bearer(User user) {
		return "Bearer " + tokenService.issueAccessToken(user).token();
	}
}
