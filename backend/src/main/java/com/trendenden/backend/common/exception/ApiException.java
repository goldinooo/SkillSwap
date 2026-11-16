package com.trendenden.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for every error the backend raises on purpose.
 *
 * <p>Throwing this (or a subclass) is how a service says "this request cannot be
 * completed, and here is the HTTP status and code to answer with". The service never
 * builds a {@link org.springframework.http.ResponseEntity} itself; {@link GlobalExceptionHandler}
 * turns the exception into the response.
 *
 * <p>It extends {@link RuntimeException}, not {@link Exception}, for two reasons:
 * callers are not forced to declare it in every signature, and Spring's
 * {@code @Transactional} rolls back on unchecked exceptions by default. A checked
 * exception would commit the transaction on the way out, which is exactly wrong when
 * the thrown error means "this write must not happen".
 */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;

	public ApiException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public ApiException(HttpStatus status, String code, String message, Throwable cause) {
		super(message, cause);
		this.status = status;
		this.code = code;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}
}
