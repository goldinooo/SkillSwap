package com.trendenden.backend.publicapi.security;

import com.trendenden.backend.common.security.AuthenticatedUser;
import com.trendenden.backend.publicapi.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Authenticates a Public API request from its {@code X-API-Key} header.
 *
 * <p>Built by {@link PublicApiSecurityConfig} rather than annotated as a component: Spring
 * Boot registers every {@code Filter} bean against all requests, so a {@code @Component}
 * filter added to a chain as well would run twice, and this one would then also apply to
 * the JWT-protected endpoints.
 *
 * <p>Like the JWT filter, a bad key leaves the context empty and lets the chain continue;
 * the 401 is produced by the entry point so that it carries the standard error shape.
 */
public class ApiKeyAuthFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-API-Key";

	/** Request attribute carrying the key id, so the rate limiter can bucket per key. */
	public static final String KEY_ID_ATTRIBUTE = "publicapi.keyId";

	private final ApiKeyService apiKeyService;

	public ApiKeyAuthFilter(ApiKeyService apiKeyService) {
		this.apiKeyService = apiKeyService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String presented = request.getHeader(HEADER);

		if (presented != null && !presented.isBlank()) {
			apiKeyService.authenticate(presented).ifPresent(apiKey -> {
				var user = apiKey.getUser();
				AuthenticatedUser principal = new AuthenticatedUser(
						user.getId(), user.getUsername(), user.getRole());
				SecurityContextHolder.getContext().setAuthentication(
						new UsernamePasswordAuthenticationToken(
								principal, null, principal.authorities()));
				request.setAttribute(KEY_ID_ATTRIBUTE, apiKey.getId());
			});
		}

		filterChain.doFilter(request, response);
	}
}
