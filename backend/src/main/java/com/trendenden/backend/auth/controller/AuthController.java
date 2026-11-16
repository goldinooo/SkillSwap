package com.trendenden.backend.auth.controller;

import com.trendenden.backend.auth.dto.LoginRequest;
import com.trendenden.backend.auth.dto.LoginResponse;
import com.trendenden.backend.auth.dto.RefreshRequest;
import com.trendenden.backend.auth.dto.TwoFactorLoginRequest;
import com.trendenden.backend.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every endpoint here is reachable without a token, since these are the endpoints that
 * hand tokens out. {@code SecurityConfig} permits {@code /api/auth/**} for that reason.
 */
@RestController
@RequestMapping("/api/auth")
@SecurityRequirements
public class AuthController {

	private final AuthService authService;

	public AuthController(AuthService authService) {
		this.authService = authService;
	}

	@PostMapping("/login")
	@Operation(summary = "Exchange email and password for tokens, or for a 2FA challenge")
	public LoginResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/2fa")
	@Operation(summary = "Finish a login that required two-factor authentication")
	public LoginResponse twoFactor(@Valid @RequestBody TwoFactorLoginRequest request) {
		return authService.completeTwoFactorLogin(request);
	}

	@PostMapping("/refresh")
	@Operation(summary = "Exchange a refresh token for a new token pair")
	public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request.refreshToken());
	}

	/** Idempotent: an unknown or already-revoked token still answers 204. */
	@PostMapping("/logout")
	@Operation(summary = "Revoke a refresh token")
	public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest request) {
		authService.logout(request.refreshToken());
		return ResponseEntity.noContent().build();
	}
}
