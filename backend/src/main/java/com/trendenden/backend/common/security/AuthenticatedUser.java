package com.trendenden.backend.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * The caller, as resolved from a verified token. This is the object stored as the Spring
 * Security principal, so {@code @AuthenticationPrincipal AuthenticatedUser me} works in
 * any controller.
 *
 * <p>It deliberately holds only what a token carries and what authorisation needs. It is
 * not the {@code User} entity: no email, no password hash, no database round-trip on
 * every request.
 *
 * @see TokenParser the contract that produces it
 */
public record AuthenticatedUser(UUID id, String username, Role role) {

	/**
	 * Authorities for this user, in the form Spring Security expects.
	 *
	 * <p>The {@code ROLE_} prefix is not decoration. {@code hasRole("ADMIN")} looks for an
	 * authority literally named {@code ROLE_ADMIN}, while {@code hasAuthority("ADMIN")}
	 * looks for {@code ADMIN}. Mixing the two is the most common reason a correct-looking
	 * rule denies everyone.
	 */
	public Collection<? extends GrantedAuthority> authorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
