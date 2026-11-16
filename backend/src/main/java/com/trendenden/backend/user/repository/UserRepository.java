package com.trendenden.backend.user.repository;

import com.trendenden.backend.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * All lookups take already-lowercased values. V10's
 * {@code CHECK (email = lower(email))} guarantees nothing non-lowercase was ever stored,
 * so a plain equality match is a correct case-insensitive comparison and still uses the
 * unique index.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	Optional<User> findByUsername(String username);

	boolean existsByEmail(String email);

	boolean existsByUsername(String username);

	Page<User> findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
			String displayName, String username, Pageable pageable);
}
