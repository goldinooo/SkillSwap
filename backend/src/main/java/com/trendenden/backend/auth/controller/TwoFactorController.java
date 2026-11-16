package com.trendenden.backend.auth.controller;

import com.trendenden.backend.auth.dto.TotpConfirmRequest;
import com.trendenden.backend.auth.dto.TotpDisableRequest;
import com.trendenden.backend.auth.dto.TotpEnrollResponse;
import com.trendenden.backend.auth.service.TwoFactorService;
import com.trendenden.backend.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Managing your own 2FA, which requires being signed in already.
 *
 * <p>Mapped under {@code /api/account} rather than {@code /api/auth}, because
 * {@code SecurityConfig} permits all of {@code /api/auth/**} without a token and these
 * three endpoints must not be reachable that way.
 */
@RestController
@RequestMapping("/api/account/2fa")
public class TwoFactorController {

	private final TwoFactorService twoFactorService;
	private final CurrentUser currentUser;

	public TwoFactorController(TwoFactorService twoFactorService, CurrentUser currentUser) {
		this.twoFactorService = twoFactorService;
		this.currentUser = currentUser;
	}

	@PostMapping("/enroll")
	@Operation(summary = "Start 2FA enrolment and get a secret plus otpauth URI")
	public TotpEnrollResponse enroll() {
		return twoFactorService.startEnrolment(currentUser.currentUserId());
	}

	@PostMapping("/confirm")
	@Operation(summary = "Confirm enrolment with a code and switch 2FA on")
	public ResponseEntity<Void> confirm(@Valid @RequestBody TotpConfirmRequest request) {
		twoFactorService.confirmEnrolment(currentUser.currentUserId(), request);
		return ResponseEntity.noContent().build();
	}

	@DeleteMapping
	@Operation(summary = "Turn 2FA off, re-confirming the password")
	public ResponseEntity<Void> disable(@Valid @RequestBody TotpDisableRequest request) {
		twoFactorService.disable(currentUser.currentUserId(), request);
		return ResponseEntity.noContent().build();
	}
}
