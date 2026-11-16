package com.trendenden.backend.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * The Next.js frontend is served from a different origin, so without this every call from
 * the browser fails the preflight check and the console fills with CORS errors. The
 * subject requires a console with zero errors.
 */
@Configuration
public class CorsConfig {

	private final List<String> allowedOrigins;

	public CorsConfig(@Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// Listed explicitly, never "*". The wildcard is illegal together with
		// allowCredentials(true) and browsers reject the response outright. Keeping the
		// list explicit also means a stray origin cannot read authenticated responses.
		configuration.setAllowedOrigins(allowedOrigins);

		configuration.setAllowedMethods(
				List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-API-Key"));

		// Needed only for cookie-carried credentials. The access token travels in the
		// Authorization header, but the refresh token may move to a cookie later, and
		// turning this on now avoids a breaking change then.
		configuration.setAllowCredentials(true);

		// Caches the preflight answer, so the browser stops sending an OPTIONS request
		// before every single call.
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
