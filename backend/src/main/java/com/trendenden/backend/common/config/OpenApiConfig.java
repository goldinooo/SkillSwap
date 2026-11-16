package com.trendenden.backend.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares how the API is authenticated so that springdoc renders an "Authorize" button.
 * Without a declared scheme, Swagger UI sends no Authorization header and every protected
 * endpoint answers 401, which makes the generated docs useless for trying anything out.
 *
 * <p>Two schemes: a bearer JWT for the main API, and {@code X-API-Key} for the Public API
 * chain. {@code addSecurityItem} applies the JWT one globally; {@code publicapi/} overrides
 * it per controller with {@code @SecurityRequirement(name = "apiKeyAuth")}.
 */
@Configuration
public class OpenApiConfig {

	private static final String BEARER_SCHEME = "bearerAuth";
	private static final String API_KEY_SCHEME = "apiKeyAuth";

	@Bean
	public OpenAPI skillSwapOpenApi() {
		return new OpenAPI()
				.info(new Info()
						.title("SkillSwap API")
						.version("v1")
						.description("Peer-to-peer skill exchange: accounts, matching, "
								+ "classes, chat and reputation."))
				// Applies the scheme to every operation, so only the genuinely public
				// endpoints need to opt out rather than every protected one opting in.
				.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
				.components(new Components()
						.addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT"))
						// The Public API chain authenticates from this header instead.
						.addSecuritySchemes(API_KEY_SCHEME, new SecurityScheme()
								.type(SecurityScheme.Type.APIKEY)
								.in(SecurityScheme.In.HEADER)
								.name("X-API-Key")));
	}
}
