package com.trendenden.backend.auth.entity;

import com.trendenden.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Maps the {@code oauth} table from V10: one provider identity linked to one account. */
@Entity
@Table(name = "oauth")
public class OAuthAccount {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private OAuthProvider provider;

	/**
	 * The provider's own identifier: {@code sub} for Google, {@code id} for 42. Never the
	 * email, which users change at the provider; the id never changes.
	 */
	@Column(name = "provider_user_id", nullable = false, length = 128)
	private String providerUserId;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected OAuthAccount() {
	}

	public OAuthAccount(User user, OAuthProvider provider, String providerUserId) {
		this.user = user;
		this.provider = provider;
		this.providerUserId = providerUserId;
	}

	public UUID getId() {
		return id;
	}

	public User getUser() {
		return user;
	}

	public OAuthProvider getProvider() {
		return provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
