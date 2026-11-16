package com.trendenden.backend.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Signing and lifetime settings, all from the environment.
 *
 * @param secret     HMAC key. At least 32 bytes, or HS256 cannot be used at all.
 * @param issuer     written as the {@code iss} claim and checked on parse
 * @param accessTtl  deliberately short: an access JWT cannot be revoked, so its lifetime
 *                   is the whole window an attacker gets from a stolen one
 * @param refreshTtl long, because this one is stored and can be revoked
 * @param twoFactorChallengeTtl lifetime of the interim token handed out between a correct
 *                   password and a correct TOTP code
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
		String secret,
		String issuer,
		Duration accessTtl,
		Duration refreshTtl,
		Duration twoFactorChallengeTtl
) {
}
