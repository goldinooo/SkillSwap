package com.trendenden.backend.publicapi.security;

import com.trendenden.backend.common.security.RestAuthErrorHandler;
import com.trendenden.backend.publicapi.service.ApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

/**
 * A second filter chain, for {@code /api/v1/public/**} only.
 *
 * <p>The Public API authenticates with {@code X-API-Key}, the rest of the backend with a
 * JWT. A separate chain keeps the two mechanisms from leaking into each other: an API key
 * cannot reach a JWT endpoint, and a JWT cannot be spent against a metered one.
 *
 * <p>{@code @Order(1)} on the bean method puts it ahead of {@code SecurityConfig}'s chain,
 * which is {@code @Order(2)} and matches everything else. Spring Security uses the first
 * chain whose matcher accepts the request, and refuses to start at all if an any-request
 * chain is registered before a narrower one.
 */
@Configuration
public class PublicApiSecurityConfig {

	// @Order has to sit on the bean method, not on the configuration class: Spring
	// Security sorts the SecurityFilterChain beans themselves. With it on the class the
	// chain took the default lowest precedence, landed behind SecurityConfig's catch-all,
	// and startup failed with UnreachableFilterChainException.
	@Bean
	@Order(1)
	public SecurityFilterChain publicApiFilterChain(HttpSecurity http,
			ApiKeyService apiKeyService,
			RestAuthErrorHandler authErrorHandler,
			CorsConfigurationSource corsConfigurationSource,
			ObjectMapper objectMapper,
			@Value("${app.publicapi.requests-per-minute}") long requestsPerMinute)
			throws Exception {

		http
				.securityMatcher("/api/v1/public/**")
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authErrorHandler)
						.accessDeniedHandler(authErrorHandler))
				.authorizeHttpRequests(requests -> requests
						// Lets a caller confirm the API is up, and their key works, before
						// spending quota on a real call.
						.requestMatchers("/api/v1/public/health").permitAll()
						.anyRequest().authenticated())

				// Order matters: the key has to be identified before the limiter can
				// decide which bucket to charge.
				.addFilterBefore(new ApiKeyAuthFilter(apiKeyService),
						UsernamePasswordAuthenticationFilter.class)
				.addFilterAfter(new RateLimitFilter(objectMapper, requestsPerMinute),
						ApiKeyAuthFilter.class)

				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);

		return http.build();
	}
}
