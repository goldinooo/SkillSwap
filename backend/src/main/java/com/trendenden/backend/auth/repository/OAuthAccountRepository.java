package com.trendenden.backend.auth.repository;

import com.trendenden.backend.auth.entity.OAuthAccount;
import com.trendenden.backend.auth.entity.OAuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OAuthAccountRepository extends JpaRepository<OAuthAccount, UUID> {

	/** The provider login lookup, served by {@code uq_oauth_provider_identity}. */
	Optional<OAuthAccount> findByProviderAndProviderUserId(
			OAuthProvider provider, String providerUserId);

	/** Served by {@code uq_oauth_user_provider}, which indexes user_id first. */
	List<OAuthAccount> findByUserId(UUID userId);

	boolean existsByUserIdAndProvider(UUID userId, OAuthProvider provider);
}
