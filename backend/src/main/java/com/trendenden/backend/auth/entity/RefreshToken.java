package com.trendenden.backend.auth.entity;

import com.trendenden.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps {@code refresh_tokens} from V10.
 *
 * <p>Only the SHA-256 digest of the token is kept, so this table is useless to anyone who
 * steals a database dump.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	// LAZY so loading a token for verification does not also select the whole user row.
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "token_hash", nullable = false, unique = true, length = 255)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private Instant expiresAt;

	/** Set on logout, on rotation, and on every token of a user whose token was reused. */
	@Column(name = "revoked_at")
	private Instant revokedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected RefreshToken() {
	}

	public RefreshToken(User user, String tokenHash, Instant expiresAt) {
		this.user = user;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	/** Idempotent: re-revoking keeps the original timestamp. */
	public void revoke(Instant at) {
		if (this.revokedAt == null) {
			this.revokedAt = at;
		}
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public boolean isExpired(Instant at) {
		return expiresAt.isBefore(at);
	}

	/** True only for a token that is both unrevoked and unexpired. */
	public boolean isUsable(Instant at) {
		return !isRevoked() && !isExpired(at);
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getTokenHash() {
		return tokenHash;
	}

	public Instant getExpiresAt() {
		return expiresAt;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
