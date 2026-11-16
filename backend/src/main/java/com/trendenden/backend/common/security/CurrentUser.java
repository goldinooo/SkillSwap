package com.trendenden.backend.common.security;

import com.trendenden.backend.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Reads the caller out of the security context.
 *
 * <p>Injected by Retahri and Nisrine instead of touching {@link SecurityContextHolder}
 * directly, so the way a request is identified can change in one place. This is the
 * {@code CurrentUser} contract from TASKS.md, with {@code UUID} rather than {@code Long}
 * now that V10 uses UUID primary keys.
 */
@Component
public class CurrentUser {

	/**
	 * @return the caller's id
	 * @throws UnauthorizedException if nobody is authenticated. On an endpoint that
	 *         {@code SecurityConfig} already requires authentication for this cannot
	 *         happen, so it signals a configuration mistake rather than a client error.
	 */
	public UUID currentUserId() {
		return currentUser().id();
	}

	/**
	 * @throws UnauthorizedException if nobody is authenticated
	 */
	public AuthenticatedUser currentUser() {
		return findCurrentUser().orElseThrow(() -> new UnauthorizedException(
				"UNAUTHENTICATED", "Authentication is required."));
	}

	/** For endpoints that serve both anonymous and signed-in callers. */
	public Optional<AuthenticatedUser> findCurrentUser() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return Optional.empty();
		}
		// Spring installs an AnonymousAuthenticationToken for unauthenticated requests,
		// and it reports isAuthenticated() == true with "anonymousUser" as principal.
		// Checking the principal type is what actually separates the two.
		return authentication.getPrincipal() instanceof AuthenticatedUser user
				? Optional.of(user)
				: Optional.empty();
	}
}
