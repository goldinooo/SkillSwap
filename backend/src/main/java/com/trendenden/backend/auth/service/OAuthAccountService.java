package com.trendenden.backend.auth.service;

import com.trendenden.backend.auth.entity.OAuthAccount;
import com.trendenden.backend.auth.entity.OAuthProvider;
import com.trendenden.backend.auth.repository.OAuthAccountRepository;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;

/** Resolves a provider identity to a local account, creating or linking as needed. */
@Service
public class OAuthAccountService {

	private static final Logger log = LoggerFactory.getLogger(OAuthAccountService.class);

	private static final int MAX_USERNAME_LENGTH = 50;

	private final OAuthAccountRepository oauthAccountRepository;
	private final UserRepository userRepository;

	public OAuthAccountService(OAuthAccountRepository oauthAccountRepository,
			UserRepository userRepository) {
		this.oauthAccountRepository = oauthAccountRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Three cases, in order:
	 * <ol>
	 *   <li>the identity is already linked, so return its user</li>
	 *   <li>the email belongs to an existing account, so link the provider to it</li>
	 *   <li>neither, so create an account</li>
	 * </ol>
	 *
	 * <p>Case 2 is what stops a user who registered with a password and later clicks
	 * "Sign in with Google" from ending up with two accounts and a lost history. It is
	 * only safe because the provider has already verified the address; trusting an
	 * unverified one would let anyone claim an account by signing up at a provider with
	 * someone else's email.
	 */
	@Transactional
	public User resolve(OAuthProvider provider, String providerUserId, String email,
			String displayName) {

		Optional<OAuthAccount> existingLink =
				oauthAccountRepository.findByProviderAndProviderUserId(provider, providerUserId);
		if (existingLink.isPresent()) {
			return existingLink.get().getUser();
		}

		String normalisedEmail = email.trim().toLowerCase(Locale.ROOT);

		Optional<User> byEmail = userRepository.findByEmail(normalisedEmail);
		if (byEmail.isPresent()) {
			User user = byEmail.get();
			// uq_oauth_user_provider allows one link per provider per user, so re-linking
			// would hit the constraint.
			if (!oauthAccountRepository.existsByUserIdAndProvider(user.getId(), provider)) {
				oauthAccountRepository.save(
						new OAuthAccount(user, provider, providerUserId));
				log.info("Linked {} identity to existing user {}", provider, user.getId());
			}
			// The provider vouched for the address, so the account is verified now.
			user.markEmailVerified();
			return user;
		}

		String username = generateUsername(normalisedEmail, displayName);
		String resolvedDisplayName = (displayName == null || displayName.isBlank())
				? username
				: displayName.trim();

		User created = userRepository.save(
				User.createFromProvider(username, normalisedEmail, resolvedDisplayName));
		oauthAccountRepository.save(new OAuthAccount(created, provider, providerUserId));
		log.info("Created user {} from {} identity", created.getId(), provider);
		return created;
	}

	/**
	 * Providers do not supply anything that satisfies our username rules, so one is
	 * derived: lowercase, only the characters V10 and {@code RegisterRequest} allow, and a
	 * numeric suffix until it is free.
	 */
	private String generateUsername(String email, String displayName) {
		String base = sanitise(email.split("@")[0]);
		if (base.length() < 3) {
			base = sanitise(displayName == null ? "" : displayName);
		}
		if (base.length() < 3) {
			base = "user";
		}
		base = base.substring(0, Math.min(base.length(), MAX_USERNAME_LENGTH - 5));

		if (!userRepository.existsByUsername(base)) {
			return base;
		}
		for (int suffix = 1; suffix < 10_000; suffix++) {
			String candidate = base + suffix;
			if (!userRepository.existsByUsername(candidate)) {
				return candidate;
			}
		}
		// 10,000 collisions on one base is not a real scenario; failing loudly beats
		// returning something that will violate the unique index.
		throw new IllegalStateException("Could not derive a free username from " + base);
	}

	private String sanitise(String value) {
		return value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]", "");
	}
}
