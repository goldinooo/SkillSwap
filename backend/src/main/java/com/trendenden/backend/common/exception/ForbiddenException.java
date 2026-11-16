package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 403. The caller is identified but not allowed: a USER hitting an ADMIN endpoint,
 * or editing a profile that is not theirs.
 */
public class ForbiddenException extends ApiException {

	public ForbiddenException(String code, String message) {
		super(HttpStatus.FORBIDDEN, code, message);
	}
}
