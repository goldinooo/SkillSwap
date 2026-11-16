package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 409. The request clashes with data that already exists: an email already registered,
 * an API key name already used, 2FA enrolled twice.
 */
public class ConflictException extends ApiException {

	public ConflictException(String code, String message) {
		super(HttpStatus.CONFLICT, code, message);
	}
}
