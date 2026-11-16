package com.trendenden.backend.auth.service;

import com.trendenden.backend.auth.config.JwtProperties;
import com.trendenden.backend.auth.entity.RefreshToken;
import com.trendenden.backend.auth.repository.RefreshTokenRepository;
import com.trendenden.backend.common.exception.UnauthorizedException;
import com.trendenden.backend.common.util.SecureTokens;
import com.trendenden.backend.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>Refresh tokens exist because an access JWT cannot be taken back: the server verifies
 * its signature and never looks it up, so logging out, changing a password or banning an
 * account has no effect on one already issued. Keeping the access token short-lived and
 * putting revocation on a stored refresh token is what makes those actions mean something.
 */
@Service
public class RefreshTokenService {

	private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

	private final RefreshTokenRepository repository;
	private final JwtProperties properties;

	public RefreshTokenService(RefreshTokenRepository repository, JwtProperties properties) {
		this.repository = repository;
		this.properties = properties;
	}

	/** @return the raw token, which is returned to the client and never stored */
	@Transactional
	public String issue(User user) {
		String raw = SecureTokens.randomToken();
		repository.save(new RefreshToken(user, SecureTokens.sha256Hex(raw),
				Instant.now().plus(properties.refreshTtl())));
		return raw;
	}

	/**
	 * Exchanges a refresh token for a fresh one, retiring the old one.
	 *
	 * <p>Rotation is what makes theft detectable. Each token is good for exactly one
	 * exchange, so if an already-rotated token is presented again there are two copies in
	 * circulation: the legitimate client's and a stolen one. There is no way to tell which
	 * caller is which, so every token of that user is revoked and both are forced to log
	 * in again.
	 *
	 * @return the user the token belonged to, and the replacement token
	 */
	@Transactional
	public Rotation rotate(String rawToken) {
		Instant now = Instant.now();
		RefreshToken stored = repository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
				.orElseThrow(() -> new UnauthorizedException(
						"INVALID_REFRESH_TOKEN", "That refresh token is not valid."));

		if (stored.isRevoked()) {
			// Reuse of a retired token: treat the whole family as compromised.
			int revoked = repository.revokeAllForUser(stored.getUser().getId(), now);
			log.warn("Refresh token reuse detected for user {}; revoked {} live token(s)",
					stored.getUser().getId(), revoked);
			throw new UnauthorizedException("REFRESH_TOKEN_REUSED",
					"That refresh token was already used. All sessions have been ended.");
		}

		if (stored.isExpired(now)) {
			throw new UnauthorizedException(
					"REFRESH_TOKEN_EXPIRED", "That refresh token has expired.");
		}

		stored.revoke(now);
		User user = stored.getUser();
		return new Rotation(user, issue(user));
	}

	/** Logout. Unknown or already-revoked tokens succeed silently: the goal is reached. */
	@Transactional
	public void revoke(String rawToken) {
		repository.findByTokenHash(SecureTokens.sha256Hex(rawToken))
				.ifPresent(token -> token.revoke(Instant.now()));
	}

	/** Logout everywhere. Also the correct response to a password change. */
	@Transactional
	public int revokeAllForUser(UUID userId) {
		return repository.revokeAllForUser(userId, Instant.now());
	}

	public record Rotation(User user, String refreshToken) {
	}
}
