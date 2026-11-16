package com.trendenden.backend.auth.service;

import com.trendenden.backend.auth.dto.LoginRequest;
import com.trendenden.backend.auth.dto.LoginResponse;
import com.trendenden.backend.auth.dto.TwoFactorLoginRequest;
import com.trendenden.backend.common.exception.UnauthorizedException;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

	/**
	 * A valid BCrypt hash of a value nobody knows, used to spend the same time verifying a
	 * password for an address that does not exist. Without it, a missing account answers
	 * measurably faster than a wrong password, and the timing alone reveals which addresses
	 * are registered.
	 */
	private static final String DUMMY_HASH =
			"$2a$10$7EqJtq98hPqEX7fNZaFWoOa0y9pGH0YVYQxWTCRQ8t1sI1Nz9kQ4u";

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;
	private final RefreshTokenService refreshTokenService;
	private final TotpService totpService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
			TokenService tokenService, RefreshTokenService refreshTokenService,
			TotpService totpService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
		this.refreshTokenService = refreshTokenService;
		this.totpService = totpService;
	}

	@Transactional
	public LoginResponse login(LoginRequest request) {
		Optional<User> found = userRepository.findByEmail(
				request.email().trim().toLowerCase(Locale.ROOT));

		// Every failure below answers with the same code and message. Distinguishing
		// "no such email" from "wrong password" would let anyone test an address list
		// against the platform and learn who is registered.
		if (found.isEmpty()) {
			passwordEncoder.matches(request.password(), DUMMY_HASH);
			throw invalidCredentials();
		}

		User user = found.get();

		// An account created through Google or 42 has no password hash. Without this
		// guard, PasswordEncoder.matches(raw, null) is reached with a null hash.
		if (user.getPasswordHash() == null) {
			passwordEncoder.matches(request.password(), DUMMY_HASH);
			throw invalidCredentials();
		}

		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw invalidCredentials();
		}

		if (user.isTwoFactorEnabled()) {
			// No session yet: the interim token only unlocks the TOTP endpoint.
			return LoginResponse.twoFactorRequired(
					tokenService.issueTwoFactorChallenge(user).token());
		}

		return completeLogin(user);
	}

	/** Second half of a 2FA login: the challenge token plus a current TOTP code. */
	@Transactional
	public LoginResponse completeTwoFactorLogin(TwoFactorLoginRequest request) {
		User user = tokenService.parseTwoFactorChallenge(request.challengeToken())
				.flatMap(userRepository::findById)
				.orElseThrow(() -> new UnauthorizedException("INVALID_CHALLENGE",
						"That two-factor challenge is not valid or has expired."));

		// Guards against a challenge issued before 2FA was turned off.
		if (!user.isTwoFactorEnabled()) {
			throw new UnauthorizedException("TWO_FACTOR_NOT_ENABLED",
					"Two-factor authentication is not enabled for this account.");
		}

		if (!totpService.verify(user.getTwoFactorSecret(), request.code())) {
			throw new UnauthorizedException("INVALID_TOTP_CODE", "That code is not valid.");
		}

		return completeLogin(user);
	}

	@Transactional
	public LoginResponse refresh(String rawRefreshToken) {
		RefreshTokenService.Rotation rotation = refreshTokenService.rotate(rawRefreshToken);
		TokenService.IssuedToken access = tokenService.issueAccessToken(rotation.user());
		return LoginResponse.completed(
				access.token(), access.expiresAt(), rotation.refreshToken());
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		refreshTokenService.revoke(rawRefreshToken);
	}

	private LoginResponse completeLogin(User user) {
		user.touchLastSeen(Instant.now());
		TokenService.IssuedToken access = tokenService.issueAccessToken(user);
		return LoginResponse.completed(
				access.token(), access.expiresAt(), refreshTokenService.issue(user));
	}

	private UnauthorizedException invalidCredentials() {
		return new UnauthorizedException(
				"INVALID_CREDENTIALS", "Email or password is incorrect.");
	}
}
