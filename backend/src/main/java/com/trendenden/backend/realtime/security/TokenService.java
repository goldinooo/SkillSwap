package com.trendenden.backend.realtime.security;

import java.util.Optional;

// TEMPORARY: this belongs to Risattou's auth/ package. Delete it and import hers once her PR is merged.
public interface TokenService {

    /**
     * @param token the raw JWT, WITHOUT the "Bearer " prefix
     * @return the user if the token is valid, Optional.empty() otherwise
     *         (expired, malformed, unknown user). Never throws.
     */
    Optional<AuthenticatedUser> parse(String token);
}
