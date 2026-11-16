package com.trendenden.backend.auth.service;

import com.trendenden.backend.auth.dto.TotpConfirmRequest;
import com.trendenden.backend.auth.dto.TotpDisableRequest;
import com.trendenden.backend.auth.dto.TotpEnrollResponse;
import com.trendenden.backend.common.exception.BadRequestException;
import com.trendenden.backend.common.exception.ConflictException;
import com.trendenden.backend.common.exception.UnauthorizedException;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enrolment is two calls on purpose.
 *
 * <p>{@link #startEnrolment} hands out a secret but leaves {@code two_factor_enabled}
 * false; {@link #confirmEnrolment} turns it on only after a code proves the authenticator
 * app really stored it. Enabling on the first call would lock out anyone whose scan
 * silently failed.
 */
@Service
public class TwoFactorService {

	private final UserService userService;
	private final TotpService totpService;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;

	public TwoFactorService(UserService userService, TotpService totpService,
			PasswordEncoder passwordEncoder, RefreshTokenService refreshTokenService) {
		this.userService = userService;
		this.totpService = totpService;
		this.passwordEncoder = passwordEncoder;
		this.refreshTokenService = refreshTokenService;
	}

	@Transactional
	public TotpEnrollResponse startEnrolment(UUID userId) {
		User user = userService.getById(userId);
		if (user.isTwoFactorEnabled()) {
			throw new ConflictException("TWO_FACTOR_ALREADY_ENABLED",
					"Two-factor authentication is already enabled.");
		}

		String secret = totpService.generateSecret();

		// Stored encrypted but with the flag still off, so an abandoned enrolment leaves
		// the account exactly as reachable as before.
		user.stageTwoFactorSecret(totpService.encrypt(secret));

		return new TotpEnrollResponse(secret,
				totpService.buildOtpAuthUri(secret, user.getEmail()));
	}

	@Transactional
	public void confirmEnrolment(UUID userId, TotpConfirmRequest request) {
		User user = userService.getById(userId);
		if (user.isTwoFactorEnabled()) {
			throw new ConflictException("TWO_FACTOR_ALREADY_ENABLED",
					"Two-factor authentication is already enabled.");
		}

		// Verified against the secret the caller echoes back, then re-encrypted from that
		// same value, so a confirmed enrolment can only ever match what was just proven.
		if (!totpService.verifyPlain(request.secret(), request.code())) {
			throw new BadRequestException("INVALID_TOTP_CODE",
					"That code does not match the secret.");
		}

		user.enableTwoFactor(totpService.encrypt(request.secret()));

		// Other sessions were established without a second factor. Ending them means
		// enabling 2FA actually protects the account rather than only new logins.
		refreshTokenService.revokeAllForUser(userId);
	}

	@Transactional
	public void disable(UUID userId, TotpDisableRequest request) {
		User user = userService.getById(userId);

		// The password is re-checked because the access token alone is not enough: if one
		// were stolen, turning off the second factor would be the attacker's first move.
		if (user.getPasswordHash() == null
				|| !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw new UnauthorizedException("INVALID_CREDENTIALS", "Password is incorrect.");
		}

		user.disableTwoFactor();
		refreshTokenService.revokeAllForUser(userId);
	}
}
