package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 400. The request is well-formed but asks for something impossible: a TOTP code that
 * does not verify, a state transition that is not allowed. Use bean validation
 * ({@code @NotBlank}, {@code @Email}) for shape problems instead; those already produce
 * a 400 with per-field detail.
 */
public class BadRequestException extends ApiException {

	public BadRequestException(String code, String message) {
		super(HttpStatus.BAD_REQUEST, code, message);
	}
}
