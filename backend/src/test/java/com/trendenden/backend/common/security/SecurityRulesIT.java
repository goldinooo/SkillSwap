package com.trendenden.backend.common.security;

import com.trendenden.backend.auth.service.TokenService;
import com.trendenden.backend.common.IntegrationTest;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import com.trendenden.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The rules in {@code SecurityConfig}, exercised through the real filter chain. */
@IntegrationTest
class SecurityRulesIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserService userService;

	@Autowired
	UserRepository userRepository;

	@Autowired
	TokenService tokenService;

	/**
	 * These 401s come from {@code RestAuthErrorHandler}, not from the advice: the security
	 * filters run before the DispatcherServlet. The shape has to match anyway.
	 */
	@Test
	void anUnauthenticatedRequestIs401InTheStandardShape() throws Exception {
		mvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.path").value("/api/users/me"))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());
	}

	/** USER against an ADMIN path: valid identity, insufficient authority. */
	@Test
	void aUserRoleOnAnAdminPathIs403InTheStandardShape() throws Exception {
		User user = userService.register(new RegisterRequest(
				"plainuser", "plainuser@example.com", "correct-horse-battery", null));

		mvc.perform(get("/api/admin/anything").header("Authorization", bearer(user)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	/**
	 * Passes only because the authority is built as {@code ROLE_ADMIN}:
	 * {@code hasRole("ADMIN")} looks for exactly that name, so a bare "ADMIN" authority
	 * would give an admin a 403.
	 */
	@Test
	void anAdminRoleClearsTheAdminRuleAndFallsThroughToNotFound() throws Exception {
		User admin = User.createFromProvider("theadmin", "theadmin@example.com", "Admin");
		admin.changeRole(Role.ADMIN);
		userRepository.saveAndFlush(admin);

		mvc.perform(get("/api/admin/anything").header("Authorization", bearer(admin)))
				// No such endpoint exists yet, so clearing authorisation lands on the
				// NoResourceFoundException handler instead of a 403.
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void theAuthEndpointsAreOpen() throws Exception {
		mvc.perform(post("/api/auth/login").contentType("application/json")
						.content("""
								{"email":"nobody@example.com","password":"whatever"}"""))
				// Reached the handler rather than being stopped by the chain.
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	/** A browser holding a stale token must still be able to use a public endpoint. */
	@Test
	void aStaleTokenDoesNotBreakAnOpenEndpoint() throws Exception {
		mvc.perform(get("/api/users/availability")
						.param("username", "someone")
						.header("Authorization", "Bearer expired.nonsense.token"))
				.andExpect(status().isNoContent());
	}

	/** With CSRF on, this would be a 403 before authentication was even attempted. */
	@Test
	void csrfIsOffSoAPostWithoutATokenIs401NotForbidden() throws Exception {
		mvc.perform(post("/api/users/me"))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * Both forms of the document, not just the JSON one. "/v3/api-docs/**" does not
	 * cover "/v3/api-docs.yaml": the wildcard matches child segments, and the .yaml
	 * form is a sibling path, so it answered 401 until it was listed explicitly.
	 */
	@Test
	void docsAndHealthAreReachable() throws Exception {
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs.yaml")).andExpect(status().isOk());
		mvc.perform(get("/actuator/health")).andExpect(status().isOk());
	}

	/**
	 * Only {@code health} is opened. The rest of actuator exposes env, configprops and
	 * beans, which publish configuration and secrets.
	 */
	@Test
	void theRestOfActuatorIsNotOpen() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
		mvc.perform(get("/actuator/beans")).andExpect(status().isUnauthorized());
	}

	/** Anything added later is protected unless someone deliberately opens it. */
	@Test
	void anUnlistedPathDefaultsToDenied() throws Exception {
		mvc.perform(get("/api/some/future/feature"))
				.andExpect(status().isUnauthorized());
	}

	/** The OpenAPI document advertises both schemes the backend actually accepts. */
	@Test
	void openApiDeclaresBothSecuritySchemes() throws Exception {
		mvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme")
						.value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.apiKeyAuth.name")
						.value("X-API-Key"));
	}

	private String bearer(User user) {
		return "Bearer " + tokenService.issueAccessToken(user).token();
	}
}
