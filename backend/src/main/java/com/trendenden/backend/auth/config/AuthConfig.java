package com.trendenden.backend.auth.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Binds {@link JwtProperties}.
 *
 * <p>Registered here rather than with {@code @ConfigurationPropertiesScan} on
 * {@code BackendApplication}, so the auth feature owns its own wiring and the shared main
 * class stays untouched.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class AuthConfig {
}
