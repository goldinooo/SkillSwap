package com.trendenden.backend.user.service;

import com.trendenden.backend.common.exception.BadRequestException;
import com.trendenden.backend.common.exception.ConflictException;
import com.trendenden.backend.common.exception.NotFoundException;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.dto.UpdateProfileRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public User register(RegisterRequest request) {
		String username = normalise(request.username());
		String email = normalise(request.email());

		// A friendly 409 for the common case. It is not the real guarantee: two concurrent
		// registrations can both pass these checks, and the unique indexes from V10 stop
		// the second INSERT. GlobalExceptionHandler turns that into a 409 as well.
		if (userRepository.existsByUsername(username)) {
			throw new ConflictException("USERNAME_TAKEN", "That username is already taken.");
		}
		if (userRepository.existsByEmail(email)) {
			throw new ConflictException("EMAIL_TAKEN", "That email is already registered.");
		}

		// V10 makes display_name NOT NULL and email signup has no display name to copy,
		// so without this fallback every registration would fail on the constraint.
		String displayName = hasText(request.displayName())
				? request.displayName().trim()
				: username;

		User user = User.createLocal(username, email, displayName,
				passwordEncoder.encode(request.password()));
		return userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public User getById(UUID id) {
		return userRepository.findById(id).orElseThrow(
				() -> new NotFoundException("USER_NOT_FOUND", "No user with that id."));
	}

	@Transactional(readOnly = true)
	public User getByUsername(String username) {
		return userRepository.findByUsername(normalise(username)).orElseThrow(
				() -> new NotFoundException("USER_NOT_FOUND", "No user with that username."));
	}

	/**
	 * Ordering comes from the {@link Pageable} the caller built, never from the query
	 * method name. A derived name such as {@code findAllByOrderByCreatedAtDesc} wins over
	 * the Pageable's {@code Sort}, which silently discards the caller's sort parameter.
	 */
	@Transactional(readOnly = true)
	public Page<User> findPublic(String query, Pageable pageable) {
		if (!hasText(query)) {
			return userRepository.findAll(pageable);
		}
		String term = query.trim();
		return userRepository
				.findByDisplayNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(
						term, term, pageable);
	}

	@Transactional(readOnly = true)
	public boolean isUsernameAvailable(String username) {
		return !userRepository.existsByUsername(normalise(username));
	}

	@Transactional
	public User updateProfile(UUID userId, UpdateProfileRequest request) {
		User user = getById(userId);

		// Rejected here rather than by a regex: the JDK's zone database is the only
		// authority on what is a real zone, and a bad value would otherwise only surface
		// when the frontend tried to render a time with it.
		if (hasText(request.timezone())) {
			try {
				ZoneId.of(request.timezone().trim());
			}
			catch (DateTimeException ex) {
				throw new BadRequestException("INVALID_TIMEZONE",
						"Not a known IANA time zone id.");
			}
		}

		user.updateProfile(
				hasText(request.displayName()) ? request.displayName().trim() : null,
				trimToNull(request.firstName()),
				trimToNull(request.lastName()),
				trimToNull(request.bio()),
				trimToNull(request.avatarUrl()),
				trimToNull(request.timezone()));
		return user;
	}

	@Transactional
	public void touchLastSeen(UUID userId) {
		userRepository.findById(userId).ifPresent(user -> user.touchLastSeen(Instant.now()));
	}

	/**
	 * Lowercases and trims, so the value matches V10's
	 * {@code CHECK (email = lower(email))} and {@code CHECK (username = lower(username))}.
	 *
	 * <p>{@code Locale.ROOT} is required: the no-argument {@code toLowerCase()} uses the
	 * default locale, and in Turkish "I" lowercases to the dotless "ı". A Turkish-locale
	 * server would store a different username than the same input on any other machine.
	 */
	private String normalise(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
	}

	private boolean hasText(String value) {
		return value != null && !value.isBlank();
	}

	private String trimToNull(String value) {
		return hasText(value) ? value.trim() : null;
	}
}
