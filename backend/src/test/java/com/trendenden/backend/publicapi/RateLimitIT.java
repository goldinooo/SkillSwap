package com.trendenden.backend.publicapi;

import com.trendenden.backend.publicapi.dto.CreateApiKeyRequest;
import com.trendenden.backend.publicapi.service.ApiKeyService;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Its own context, because the quota is a startup property and the buckets live for the
 * lifetime of the filter. Three requests a minute makes the limit reachable in a test
 * without sleeping.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "app.publicapi.requests-per-minute=3")
class RateLimitIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserService userService;

	@Autowired
	ApiKeyService apiKeyService;

	@Test
	void theFourthRequestIsRejectedWithRetryAfter() throws Exception {
		String key = newKey("limited", "limited@example.com");

		for (int i = 1; i <= 3; i++) {
			mvc.perform(get("/api/v1/public/stats").header("X-API-Key", key))
					.andExpect(status().isOk())
					.andExpect(result -> assertThat(
							result.getResponse().getHeader("X-RateLimit-Remaining"))
							.isNotNull());
		}

		mvc.perform(get("/api/v1/public/stats").header("X-API-Key", key))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("RATE_LIMIT_EXCEEDED"))
				.andExpect(jsonPath("$.status").value(429))
				// A client cannot back off sensibly without being told how long.
				.andExpect(result -> assertThat(
						result.getResponse().getHeader("Retry-After")).isNotNull())
				.andExpect(result -> assertThat(
						result.getResponse().getHeader("X-RateLimit-Remaining"))
						.isEqualTo("0"));
	}

	/** Buckets are keyed per API key, so one client cannot spend another's quota. */
	@Test
	void eachKeyHasItsOwnBudget() throws Exception {
		String first = newKey("budgeta", "budgeta@example.com");
		String second = newKey("budgetb", "budgetb@example.com");

		for (int i = 1; i <= 3; i++) {
			mvc.perform(get("/api/v1/public/stats").header("X-API-Key", first))
					.andExpect(status().isOk());
		}
		mvc.perform(get("/api/v1/public/stats").header("X-API-Key", first))
				.andExpect(status().isTooManyRequests());

		// The second key is untouched by the first one exhausting itself.
		mvc.perform(get("/api/v1/public/stats").header("X-API-Key", second))
				.andExpect(status().isOk());
	}

	/** The limiter must not answer 429 before the chain has answered 401. */
	@Test
	void anUnauthenticatedRequestIsRefusedRatherThanMetered() throws Exception {
		mvc.perform(get("/api/v1/public/stats"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	private String newKey(String username, String email) {
		User user = userService.register(
				new RegisterRequest(username, email, "correct-horse-battery", null));
		return apiKeyService.create(user.getId(), new CreateApiKeyRequest("k")).key();
	}
}
