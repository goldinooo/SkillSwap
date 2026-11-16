package com.trendenden.backend.publicapi.entity;

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
 * Maps {@code api_keys} from V10. Only the SHA-256 digest of the key is stored; the key
 * itself is shown once at creation and never again.
 */
@Entity
@Table(name = "api_keys")
public class ApiKey {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, length = 60)
	private String name;

	@Column(name = "key_hash", nullable = false, unique = true, length = 64)
	private String keyHash;

	/**
	 * Soft revoke: the row stays so an audit can still show the key existed and when
	 * access was withdrawn. Every lookup must therefore test this, not row absence.
	 */
	@Column(name = "revoked_at")
	private Instant revokedAt;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ApiKey() {
	}

	public ApiKey(User user, String name, String keyHash) {
		this.user = user;
		this.name = name;
		this.keyHash = keyHash;
	}

	public void revoke(Instant at) {
		if (this.revokedAt == null) {
			this.revokedAt = at;
		}
	}

	public boolean isRevoked() {
		return revokedAt != null;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public String getName() {
		return name;
	}

	public String getKeyHash() {
		return keyHash;
	}

	public Instant getRevokedAt() {
		return revokedAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
