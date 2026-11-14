package com.trendenden.backend.realtime.security;

import java.util.Optional;
import org.springframework.stereotype.Component;

// Test-only stand-in for Risattou's TokenService: "test-user-1" -> user 1, anything else is invalid.
@Component
public class FakeTokenService implements TokenService {

    private static final String PREFIX = "test-user-";

    @Override
    public Optional<AuthenticatedUser> parse(String token) {
        if (token == null || !token.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            long id = Long.parseLong(token.substring(PREFIX.length()));
            return Optional.of(new AuthenticatedUser(id, "user" + id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
