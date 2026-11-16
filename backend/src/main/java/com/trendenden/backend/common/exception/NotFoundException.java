package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 404. The addressed resource does not exist.
 *
 * <p>Careful on endpoints that touch someone else's data: answering 403 for "exists but
 * not yours" and 404 for "does not exist" lets a caller probe which ids are real.
 * Where that matters, return this for both.
 */
public class NotFoundException extends ApiException {

	public NotFoundException(String code, String message) {
		super(HttpStatus.NOT_FOUND, code, message);
	}
}
