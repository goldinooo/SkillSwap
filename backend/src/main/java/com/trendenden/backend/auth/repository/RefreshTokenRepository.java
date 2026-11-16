package com.trendenden.backend.auth.repository;

import com.trendenden.backend.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

	/**
	 * Looks up by hash alone, including revoked and expired rows. The caller has to see a
	 * revoked row to detect reuse, so filtering it out here would hide the attack this
	 * design exists to catch.
	 */
	Optional<RefreshToken> findByTokenHash(String tokenHash);

	List<RefreshToken> findByUserIdAndRevokedAtIsNull(UUID userId);

	/**
	 * Revokes every live token of one user: used on logout-everywhere, on password change,
	 * and when a reused token shows the family is compromised.
	 *
	 * <p>A bulk JPQL update runs straight against the database and does not go through the
	 * persistence context, which makes both flags mandatory rather than decoration:
	 * <ul>
	 *   <li>{@code flushAutomatically} pushes pending entity changes out first. Without it
	 *       a token revoked in memory earlier in the same transaction is still unflushed,
	 *       so this statement cannot see it and a later flush could write the stale value
	 *       back over the result.</li>
	 *   <li>{@code clearAutomatically} evicts the now-stale managed entities. Without it a
	 *       read later in the same transaction is served from the first-level cache and
	 *       reports {@code revokedAt == null} for a row this statement just revoked, so
	 *       the token keeps working.</li>
	 * </ul>
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("update RefreshToken t set t.revokedAt = :at "
			+ "where t.user.id = :userId and t.revokedAt is null")
	int revokeAllForUser(@Param("userId") UUID userId, @Param("at") Instant at);

	/** Housekeeping: rows that are both expired and revoked can never be needed again. */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from RefreshToken t where t.expiresAt < :before and t.revokedAt is not null")
	int deleteSpent(@Param("before") Instant before);
}
