package com.trendenden.backend.publicapi.controller;

import com.trendenden.backend.common.security.CurrentUser;
import com.trendenden.backend.publicapi.dto.ApiKeyCreatedResponse;
import com.trendenden.backend.publicapi.dto.ApiKeyResponse;
import com.trendenden.backend.publicapi.dto.CreateApiKeyRequest;
import com.trendenden.backend.publicapi.service.ApiKeyService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Managing your own API keys. Reached with a JWT, not with an API key: a key must not be
 * able to mint more keys, or one leak would be permanent.
 */
@RestController
@RequestMapping("/api/account/api-keys")
public class ApiKeyController {

	private final ApiKeyService apiKeyService;
	private final CurrentUser currentUser;

	public ApiKeyController(ApiKeyService apiKeyService, CurrentUser currentUser) {
		this.apiKeyService = apiKeyService;
		this.currentUser = currentUser;
	}

	@PostMapping
	@Operation(summary = "Create an API key. The key is returned once and never again.")
	public ResponseEntity<ApiKeyCreatedResponse> create(
			@Valid @RequestBody CreateApiKeyRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(apiKeyService.create(currentUser.currentUserId(), request));
	}

	@GetMapping
	@Operation(summary = "List your API keys, without any key material")
	public List<ApiKeyResponse> list() {
		return apiKeyService.list(currentUser.currentUserId())
				.stream().map(ApiKeyResponse::from).toList();
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Revoke an API key")
	public ResponseEntity<Void> revoke(@PathVariable UUID id) {
		apiKeyService.revoke(currentUser.currentUserId(), id);
		return ResponseEntity.noContent().build();
	}
}
