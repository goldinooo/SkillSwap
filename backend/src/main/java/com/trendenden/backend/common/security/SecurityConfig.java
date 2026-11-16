package com.trendenden.backend.common.security;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Optional;

/**
 * The single place that decides who may reach which endpoint.
 *
 * @see RestAuthErrorHandler for the 401/403 bodies this chain produces
 */
@Configuration
@EnableWebSecurity
// Enables @PreAuthorize / @PostAuthorize on methods. Those denials are thrown inside the
// DispatcherServlet, so GlobalExceptionHandler's AccessDeniedException handler catches
// them; the chain-level ones below are caught by RestAuthErrorHandler instead.
@EnableMethodSecurity
public class SecurityConfig {

	/**
	 * The catch-all chain. {@code @Order(2)} leaves room for
	 * {@code publicapi/security/PublicApiSecurityConfig}, which is {@code @Order(1)} and
	 * claims {@code /api/v1/public/**} for API-key authentication. Spring Security picks
	 * the first chain whose matcher accepts a request, so without the ordering this one
	 * would swallow those paths.
	 */
	@Bean
	@Order(2)
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			ObjectProvider<TokenParser> tokenParsers,
			ObjectProvider<ClientRegistrationRepository> clientRegistrations,
			ObjectProvider<AuthenticationSuccessHandler> oauthSuccessHandlers,
			RestAuthErrorHandler authErrorHandler,
			CorsConfigurationSource corsConfigurationSource) throws Exception {

		// auth/service/TokenService implements TokenParser and arrives in step 5. Until
		// then no token can be verified, so every request stays anonymous and protected
		// endpoints answer 401 — which is the correct behaviour for a backend that cannot
		// yet validate tokens. The real bean takes over with no change here.
		TokenParser tokenParser = tokenParsers.getIfAvailable(() -> token -> Optional.empty());

		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))

				// CSRF defends cookie-based sessions, where the browser attaches
				// credentials automatically. This API authenticates from the
				// Authorization header, which a cross-site form cannot set, so the token
				// is already proof of intent.
				// If the refresh token ever moves into a cookie, CSRF has to come back
				// for the endpoint that reads it.
				.csrf(AbstractHttpConfigurer::disable)

				// No HttpSession at all: the token carries the identity. Without this
				// Spring would create a session per request and the backend would stop
				// being horizontally scalable.
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(authErrorHandler)
						.accessDeniedHandler(authErrorHandler))

				.authorizeHttpRequests(requests -> requests
						// Login, refresh and the OAuth2 redirects have to be reachable
						// without a token, or nobody could ever obtain one.
						.requestMatchers("/api/auth/**").permitAll()
						.requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()

						// Registration only. Everything else under /api/users needs a token,
						// so this is scoped to POST on the exact path.
						.requestMatchers(HttpMethod.POST, "/api/users").permitAll()

						// The signup form checks this before the user has a token.
						.requestMatchers(HttpMethod.GET, "/api/users/availability").permitAll()

						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
								"/swagger-ui.html").permitAll()

						// Liveness only. /actuator/** as a whole would expose env,
						// configprops and beans, which leak configuration and secrets.
						.requestMatchers("/actuator/health").permitAll()

						.requestMatchers("/api/admin/**").hasRole("ADMIN")

						// Default deny. Any endpoint added later is protected unless
						// someone deliberately opens it above.
						.anyRequest().authenticated())

				.addFilterBefore(new JwtAuthenticationFilter(tokenParser),
						UsernamePasswordAuthenticationFilter.class)

				// Both would otherwise be auto-enabled by the security starter: form
				// login redirects API clients to an HTML page, and basic auth pops a
				// browser dialog. Neither belongs in a token API.
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable);

		// Only wired when Google or 42 credentials are actually configured. Calling
		// oauth2Login() unconditionally needs a ClientRegistrationRepository bean, and
		// Spring only creates one when at least one registration has a client-id, so an
		// unconditional call would stop the whole application from starting on any machine
		// without those secrets.
		ClientRegistrationRepository registrations = clientRegistrations.getIfAvailable();
		AuthenticationSuccessHandler successHandler = oauthSuccessHandlers.getIfAvailable();
		if (registrations != null && successHandler != null) {
			http.oauth2Login(login -> login
					.clientRegistrationRepository(registrations)
					.successHandler(successHandler)
					// Default-deny applies here too: a failed provider login must not fall
					// through to Spring's HTML login page.
					.failureHandler(authErrorHandler::commence));
		}

		return http.build();
	}

	/**
	 * BCrypt generates a random salt per hash and stores it inside its own output
	 * ({@code $2a$10$<22-char salt><31-char hash>}), which is why V10 has no salt column.
	 *
	 * <p>The work factor stays at the default 10. Raising it makes both hashing and every
	 * login slower on purpose; it is a deliberate tuning decision, not a default to bump.
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
