package com.trendenden.backend.common.security;

/**
 * Account role, mirroring the {@code users.role} CHECK constraint in V10.
 *
 * <p>It lives in {@code common/security} rather than {@code user/entity} because the
 * authorisation path needs it (the JWT filter, {@code hasRole(...)} rules) and
 * {@code common} must not depend on a feature package. {@code user/entity/User} imports
 * it from here, which keeps the dependency pointing one way.
 *
 * <p>Persisted with {@code @Enumerated(EnumType.STRING)}. Never {@code ORDINAL}: that
 * stores 0/1, so reordering these constants would silently relabel every existing row.
 */
public enum Role {
	USER,
	ADMIN
}
