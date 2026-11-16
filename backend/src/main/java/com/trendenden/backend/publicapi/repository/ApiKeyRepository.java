package com.trendenden.backend.publicapi.repository;

import com.trendenden.backend.publicapi.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

	/**
	 * The authentication lookup. {@code AndRevokedAtIsNull} is the whole point: because
	 * revocation is a timestamp rather than a delete, {@code findByKeyHash} alone would
	 * keep accepting revoked keys.
	 */
	Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);

	/** Served by {@code uq_api_keys_user_name}, which indexes user_id first. */
	List<ApiKey> findByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<ApiKey> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndNameAndRevokedAtIsNull(UUID userId, String name);
}
