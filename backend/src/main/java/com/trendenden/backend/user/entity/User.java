package com.trendenden.backend.user.entity;

import com.trendenden.backend.common.security.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps the {@code users} table from V10.
 *
 * <p>State changes go through the named methods below rather than public setters, so the
 * CHECK constraints in V10 cannot be violated from Java: {@link #enableTwoFactor} always
 * sets a secret alongside the flag, and the username and email are only ever assigned
 * from already-normalised values.
 *
 * <p>No Lombok {@code @Data} and no {@code equals}/{@code hashCode}: generating them over
 * mutable fields breaks as soon as an entity sits in a {@code Set} and is then modified,
 * and a generated {@code toString} would print the password hash.
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	// Generated in Java, so the id is known before the INSERT and can be used to build the
	// response or link rows in the same transaction. The DEFAULT in V10 stays as a safety
	// net for hand-written SQL.
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "first_name", length = 50)
	private String firstName;

	@Column(name = "last_name", length = 50)
	private String lastName;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(name = "display_name", nullable = false, length = 255)
	private String displayName;

	@Column(nullable = false, unique = true, length = 255)
	private String email;

	/** BCrypt output. Null for accounts that only ever signed in through a provider. */
	@Column(name = "password_hash", length = 255)
	private String passwordHash;

	@Column(columnDefinition = "text")
	private String bio;

	@Column(name = "avatar_url", length = 255)
	private String avatarUrl;

	@Column(length = 50)
	private String timezone;

	@Column(name = "email_verified", nullable = false)
	private boolean emailVerified;

	@Column(name = "two_factor_enabled", nullable = false)
	private boolean twoFactorEnabled;

	/** Ciphertext, never the raw TOTP secret. */
	@Column(name = "two_factor_secret", length = 255)
	private String twoFactorSecret;

	// STRING, never ORDINAL: ORDINAL stores 0/1, so reordering the enum would silently
	// relabel every existing row.
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(name = "last_seen")
	private Instant lastSeen;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	// V10's DEFAULT now() only fires on INSERT, so the refresh has to happen here.
	@UpdateTimestamp
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	/** Required by JPA. */
	protected User() {
	}

	private User(String username, String email, String displayName, String passwordHash) {
		this.username = username;
		this.email = email;
		this.displayName = displayName;
		this.passwordHash = passwordHash;
		this.role = Role.USER;
	}

	/** Email and password signup. Values must already be normalised by the service. */
	public static User createLocal(String username, String email, String displayName,
			String passwordHash) {
		return new User(username, email, displayName, passwordHash);
	}

	/**
	 * Signup through Google or 42. {@code passwordHash} stays null, which is why V10 keeps
	 * the column nullable. The provider has already verified the address.
	 */
	public static User createFromProvider(String username, String email, String displayName) {
		User user = new User(username, email, displayName, null);
		user.emailVerified = true;
		return user;
	}

	public void updateProfile(String displayName, String firstName, String lastName,
			String bio, String avatarUrl, String timezone) {
		if (displayName != null) {
			this.displayName = displayName;
		}
		this.firstName = firstName;
		this.lastName = lastName;
		this.bio = bio;
		this.avatarUrl = avatarUrl;
		this.timezone = timezone;
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	/** Keeps flag and secret in step, mirroring {@code chk_users_2fa_secret_present}. */
	public void enableTwoFactor(String encryptedSecret) {
		if (encryptedSecret == null || encryptedSecret.isBlank()) {
			throw new IllegalArgumentException("Cannot enable 2FA without a secret");
		}
		this.twoFactorSecret = encryptedSecret;
		this.twoFactorEnabled = true;
	}

	/** Clears the flag before the secret, so the pair is never flag-on/secret-null. */
	public void disableTwoFactor() {
		this.twoFactorEnabled = false;
		this.twoFactorSecret = null;
	}

	/** Secret stored but not yet confirmed by a valid code, so the flag stays off. */
	public void stageTwoFactorSecret(String encryptedSecret) {
		this.twoFactorSecret = encryptedSecret;
		this.twoFactorEnabled = false;
	}

	/**
	 * Grants or withdraws the ADMIN role. Needed by the Advanced permissions module; the
	 * endpoint that calls it must itself be restricted to ADMIN, or anyone could promote
	 * themselves.
	 */
	public void changeRole(Role role) {
		this.role = role;
	}

	public void markEmailVerified() {
		this.emailVerified = true;
	}

	public void touchLastSeen(Instant at) {
		this.lastSeen = at;
	}

	public UUID getId() {
		return id;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getBio() {
		return bio;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public String getTimezone() {
		return timezone;
	}

	public boolean isEmailVerified() {
		return emailVerified;
	}

	public boolean isTwoFactorEnabled() {
		return twoFactorEnabled;
	}

	public String getTwoFactorSecret() {
		return twoFactorSecret;
	}

	public Role getRole() {
		return role;
	}

	public Instant getLastSeen() {
		return lastSeen;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
