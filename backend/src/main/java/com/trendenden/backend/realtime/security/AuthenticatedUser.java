package com.trendenden.backend.realtime.security;

// TEMPORARY: this belongs to Risattou's auth/ package. Delete it and import hers once her PR is merged.
public record AuthenticatedUser(Long id, String username) {}
