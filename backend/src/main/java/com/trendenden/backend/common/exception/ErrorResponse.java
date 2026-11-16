package com.trendenden.backend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The one and only error body the backend returns. Every failure, from a validation
 * error to an unexpected crash, is serialised through this record, so the frontend
 * has a single shape to parse instead of one per endpoint.
 *
 * <p>{@code code} is the machine-readable part: the frontend switches on it and picks
 * its own translated text. {@code message} is for developers and logs. Never branch
 * on {@code message} in the frontend, it is not a contract and it is not translated.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
		Instant timestamp,
		int status,
		String code,
		String message,
		String path,
		List<FieldError> fieldErrors
) {

	/** One invalid field. Only present on validation failures. */
	public record FieldError(String field, String message) {
	}

	public static ErrorResponse of(int status, String code, String message, String path) {
		return new ErrorResponse(Instant.now(), status, code, message, path, null);
	}

	public static ErrorResponse of(int status, String code, String message, String path,
			List<FieldError> fieldErrors) {
		return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
	}
}
