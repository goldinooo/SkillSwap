package com.trendenden.backend.publicapi.service;

import com.trendenden.backend.common.exception.ConflictException;
import com.trendenden.backend.common.exception.NotFoundException;
import com.trendenden.backend.common.util.SecureTokens;
import com.trendenden.backend.publicapi.dto.ApiKeyCreatedResponse;
import com.trendenden.backend.publicapi.dto.CreateApiKeyRequest;
import com.trendenden.backend.publicapi.entity.ApiKey;
import com.trendenden.backend.publicapi.repository.ApiKeyRepository;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ApiKeyService {

	/**
	 * A fixed, recognisable prefix. Secret scanners and log filters look for patterns like
	 * this, so a key pasted into a public repository or a log line can be spotted and
	 * revoked instead of sitting there unnoticed.
	 */
	private static final String KEY_PREFIX = "ssk_";

	private final ApiKeyRepository apiKeyRepository;
	private final UserService userService;

	public ApiKeyService(ApiKeyRepository apiKeyRepository, UserService userService) {
		this.apiKeyRepository = apiKeyRepository;
		this.userService = userService;
	}

	@Transactional
	public ApiKeyCreatedResponse create(UUID userId, CreateApiKeyRequest request) {
		String name = request.name().trim();

		// uq_api_keys_user_name covers revoked rows too, so this check only looks at live
		// keys and a revoked name can be reused. A collision with a revoked row would be
		// turned into a 409 by GlobalExceptionHandler anyway.
		if (apiKeyRepository.existsByUserIdAndNameAndRevokedAtIsNull(userId, name)) {
			throw new ConflictException("API_KEY_NAME_TAKEN",
					"You already have a key with that name.");
		}

		User user = userService.getById(userId);
		String raw = KEY_PREFIX + SecureTokens.randomToken();

		// The digest is stored, the raw value is returned once and then forgotten.
		ApiKey saved = apiKeyRepository.save(
				new ApiKey(user, name, SecureTokens.sha256Hex(raw)));

		return new ApiKeyCreatedResponse(
				saved.getId(), saved.getName(), raw, saved.getCreatedAt());
	}

	@Transactional(readOnly = true)
	public List<ApiKey> list(UUID userId) {
		return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional
	public void revoke(UUID userId, UUID keyId) {
		// Scoped to the owner, so one user cannot revoke another user's key by guessing
		// its id. A key belonging to someone else is reported as not found rather than
		// forbidden, which keeps id existence from leaking.
		ApiKey key = apiKeyRepository.findByIdAndUserId(keyId, userId).orElseThrow(
				() -> new NotFoundException("API_KEY_NOT_FOUND", "No such API key."));
		key.revoke(Instant.now());
	}

	/** Authentication path: hash the presented key and look for a live row. */
	@Transactional(readOnly = true)
	public Optional<ApiKey> authenticate(String presentedKey) {
		if (presentedKey == null || presentedKey.isBlank()) {
			return Optional.empty();
		}
		return apiKeyRepository.findByKeyHashAndRevokedAtIsNull(
				SecureTokens.sha256Hex(presentedKey.trim()));
	}
}
