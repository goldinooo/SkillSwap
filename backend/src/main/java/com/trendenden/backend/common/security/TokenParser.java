package com.trendenden.backend.common.security;

import java.util.Optional;

/**
 * Turns a raw access token into the caller it identifies.
 *
 * <p>This is the agreed seam between {@code common/security} and {@code auth/}. The
 * interface lives here, in the lower layer; {@code auth/service/TokenService} implements
 * it. Without that inversion, {@code common} would have to import {@code auth} and the
 * dependency would point the wrong way.
 *
 * <p>Nisrine's WebSocket CONNECT handler calls this too, so the signature is part of the
 * team contract in TASKS.md. It can be stubbed in tests with a lambda.
 */
public interface TokenParser {

	/**
	 * @param token the raw token, with any {@code Bearer } prefix already stripped
	 * @return the caller, or empty if the token is absent, malformed, expired, or its
	 *         signature does not verify. Implementations must not throw for a bad token:
	 *         "not authenticated" is a normal outcome, not an error.
	 */
	Optional<AuthenticatedUser> parse(String token);
}
