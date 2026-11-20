package com.trendenden.backend.realtime.security;

import java.util.Optional;

// TEMPORARY: until Risattou's TokenParser is merged.
// parse(token): token without "Bearer " → user if valid, empty if not.

public interface TokenService {
    Optional<AuthenticatedUser> parse(String token);
}

