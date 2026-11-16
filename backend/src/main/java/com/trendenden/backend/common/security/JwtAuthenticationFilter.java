package com.trendenden.backend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads {@code Authorization: Bearer <token>} and, if the token verifies, puts the caller
 * into the security context for the rest of the request.
 *
 * <p>It is constructed by {@link SecurityConfig} rather than annotated as a component.
 * Spring Boot auto-registers any {@code Filter} bean against every request, so a
 * {@code @Component} filter that is also added to the security chain runs twice.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final TokenParser tokenParser;

	public JwtAuthenticationFilter(TokenParser tokenParser) {
		this.tokenParser = tokenParser;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String header = request.getHeader(HttpHeaders.AUTHORIZATION);

		if (header != null && header.startsWith(BEARER_PREFIX)) {
			String token = header.substring(BEARER_PREFIX.length());

			// A bad token leaves the context empty and the chain continues. It must not
			// throw or short-circuit: a public endpoint has to keep working when the
			// browser still holds a stale token, and the 401 for a protected endpoint is
			// RestAuthErrorHandler's job, which produces the agreed ErrorResponse shape.
			tokenParser.parse(token).ifPresent(user -> {
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(user, null, user.authorities());
				authentication.setDetails(
						new WebAuthenticationDetailsSource().buildDetails(request));
				SecurityContextHolder.getContext().setAuthentication(authentication);
			});
		}

		filterChain.doFilter(request, response);
	}
}
