package com.trendenden.backend.auth.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.trendenden.backend.auth.config.JwtProperties;
import com.trendenden.backend.common.security.AuthenticatedUser;
import com.trendenden.backend.common.security.Role;
import com.trendenden.backend.common.security.TokenParser;
import com.trendenden.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * Issues and verifies access JWTs. Implements {@link TokenParser}, which is the interface
 * {@code common/security} declares, so registering this bean is what switches the JWT
 * filter from "nothing authenticates" to real authentication, with no change in
 * {@code SecurityConfig}.
 *
 * <p>Signed with HS256 using Nimbus JOSE+JWT, which arrives with
 * {@code spring-boot-starter-security-oauth2-resource-server}. No extra dependency.
 */
@Service
public class TokenService implements TokenParser {

	private static final Logger log = LoggerFactory.getLogger(TokenService.class);

	/** Marks the interim token issued between password and TOTP check. */
	static final String SCOPE_CLAIM = "scp";
	static final String SCOPE_TWO_FACTOR = "2fa";

	private static final String USERNAME_CLAIM = "username";
	private static final String ROLE_CLAIM = "role";

	/** HS256 needs a key at least as long as its output: 256 bits. */
	private static final int MIN_SECRET_BYTES = 32;

	private final JwtProperties properties;
	private final byte[] secret;

	public TokenService(JwtProperties properties) {
		this.properties = properties;
		this.secret = properties.secret().getBytes(StandardCharsets.UTF_8);

		// Checked at startup rather than on the first login. MACSigner would otherwise
		// throw on the very first token, turning a configuration mistake into a 500 in
		// production instead of a refusal to boot.
		if (this.secret.length < MIN_SECRET_BYTES) {
			throw new IllegalStateException("app.jwt.secret must be at least "
					+ MIN_SECRET_BYTES + " bytes for HS256, got " + this.secret.length);
		}
	}

	/** The short-lived token sent on every request. */
	public IssuedToken issueAccessToken(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.accessTtl());
		String token = sign(new JWTClaimsSet.Builder()
				.subject(user.getId().toString())
				.claim(USERNAME_CLAIM, user.getUsername())
				.claim(ROLE_CLAIM, user.getRole().name())
				.issuer(properties.issuer())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(expiresAt))
				.jwtID(UUID.randomUUID().toString())
				.build());
		return new IssuedToken(token, expiresAt);
	}

	/**
	 * Proves the password step passed, nothing more. {@link #parse} refuses it, so it
	 * cannot be used as an access token even though it is signed with the same key.
	 */
	public IssuedToken issueTwoFactorChallenge(User user) {
		Instant now = Instant.now();
		Instant expiresAt = now.plus(properties.twoFactorChallengeTtl());
		String token = sign(new JWTClaimsSet.Builder()
				.subject(user.getId().toString())
				.claim(SCOPE_CLAIM, SCOPE_TWO_FACTOR)
				.issuer(properties.issuer())
				.issueTime(Date.from(now))
				.expirationTime(Date.from(expiresAt))
				.jwtID(UUID.randomUUID().toString())
				.build());
		return new IssuedToken(token, expiresAt);
	}

	/** @return the user id the challenge was issued for, or empty if it is not valid */
	public Optional<UUID> parseTwoFactorChallenge(String token) {
		return claims(token)
				.filter(set -> SCOPE_TWO_FACTOR.equals(set.getClaim(SCOPE_CLAIM)))
				.flatMap(set -> subjectOf(set));
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>Returns empty for every kind of bad token rather than throwing: the JWT filter
	 * treats "not authenticated" as a normal outcome and lets the chain continue.
	 */
	@Override
	public Optional<AuthenticatedUser> parse(String token) {
		return claims(token)
				// A 2FA challenge is signed with the same key, so without this check it
				// would authenticate as a full session and skip the TOTP step entirely.
				.filter(set -> set.getClaim(SCOPE_CLAIM) == null)
				.flatMap(set -> {
					Optional<UUID> id = subjectOf(set);
					Object username = set.getClaim(USERNAME_CLAIM);
					Object role = set.getClaim(ROLE_CLAIM);
					if (id.isEmpty() || !(username instanceof String name)
							|| !(role instanceof String roleName)) {
						return Optional.empty();
					}
					try {
						return Optional.of(new AuthenticatedUser(
								id.get(), name, Role.valueOf(roleName)));
					}
					catch (IllegalArgumentException ex) {
						// A role that no longer exists in the enum. Refuse rather than
						// guess, so removing a role immediately invalidates its tokens.
						return Optional.empty();
					}
				});
	}

	private String sign(JWTClaimsSet claims) {
		try {
			SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
			jwt.sign(new MACSigner(secret));
			return jwt.serialize();
		}
		catch (JOSEException ex) {
			throw new IllegalStateException("Failed to sign JWT", ex);
		}
	}

	/** Verifies signature, issuer and expiry. Any failure yields empty. */
	private Optional<JWTClaimsSet> claims(String token) {
		if (token == null || token.isBlank()) {
			return Optional.empty();
		}
		try {
			SignedJWT jwt = SignedJWT.parse(token);

			// Signature first: nothing inside the token can be trusted before this.
			if (!jwt.verify(new MACVerifier(secret))) {
				return Optional.empty();
			}

			JWTClaimsSet set = jwt.getJWTClaimsSet();

			if (!properties.issuer().equals(set.getIssuer())) {
				return Optional.empty();
			}

			// A token with no expiry would be valid forever, so a missing claim is
			// treated as invalid rather than as "no limit".
			Date expiry = set.getExpirationTime();
			if (expiry == null || expiry.toInstant().isBefore(Instant.now())) {
				return Optional.empty();
			}
			return Optional.of(set);
		}
		catch (ParseException | JOSEException ex) {
			log.debug("Rejected a malformed or unverifiable token", ex);
			return Optional.empty();
		}
	}

	private Optional<UUID> subjectOf(JWTClaimsSet set) {
		try {
			return Optional.ofNullable(set.getSubject()).map(UUID::fromString);
		}
		catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
	}

	public record IssuedToken(String token, Instant expiresAt) {
	}
}
