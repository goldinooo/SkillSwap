package com.trendenden.backend.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Translates every exception that reaches the DispatcherServlet into an
 * {@link ErrorResponse}. This is the only place in the backend that decides what an
 * error looks like on the wire.
 *
 * <p><b>What this class does not cover.</b> Spring Security's filters run before the
 * DispatcherServlet, so a missing or invalid JWT never reaches this handler: that 401
 * is produced by the {@code AuthenticationEntryPoint} configured in
 * {@code common/security/SecurityConfig}. Both have to emit this same record, or the
 * single-shape promise breaks on exactly the responses clients hit most.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	/** Everything the backend throws deliberately. Status and code come from the exception. */
	@ExceptionHandler(ApiException.class)
	public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
		return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(
				ex.getStatus().value(), ex.getCode(), ex.getMessage(), request.getRequestURI()));
	}

	/** A {@code @Valid @RequestBody} DTO failed its constraints. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
				"Request validation failed.", request.getRequestURI(), fields));
	}

	/** A constraint on a {@code @RequestParam} or {@code @PathVariable} failed. */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException ex,
			HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = ex.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorResponse.FieldError(
								result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
				.toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
				"Request validation failed.", request.getRequestURI(), fields));
	}

	/**
	 * Bean validation raised through {@code MethodValidationInterceptor}, which is what
	 * {@code @Validated} on a class produces. Spring MVC's own parameter validation throws
	 * {@link HandlerMethodValidationException} instead, so both paths exist and only one
	 * of them is covered above; without this a {@code @Validated} service would surface as
	 * a 500.
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ErrorResponse> handleConstraintViolation(
			ConstraintViolationException ex, HttpServletRequest request) {
		List<ErrorResponse.FieldError> fields = ex.getConstraintViolations().stream()
				.map(violation -> new ErrorResponse.FieldError(
						lastNodeOf(violation.getPropertyPath().toString()),
						violation.getMessage()))
				.toList();
		return ResponseEntity.badRequest().body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "VALIDATION_FAILED",
				"Request validation failed.", request.getRequestURI(), fields));
	}

	/**
	 * A violation path is reported as "method.parameter"; only the parameter name is
	 * useful to a client, and the method name is internal detail.
	 */
	private String lastNodeOf(String propertyPath) {
		int lastDot = propertyPath.lastIndexOf('.');
		return lastDot < 0 ? propertyPath : propertyPath.substring(lastDot + 1);
	}

	/** Body was not valid JSON, or a value did not fit its target type. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		// ex.getMessage() quotes the offending JSON and names our Java classes, so it is
		// logged rather than returned.
		log.debug("Unreadable request body on {}", request.getRequestURI(), ex);
		return ResponseEntity.badRequest().body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "MALFORMED_REQUEST",
				"Request body is missing or not valid JSON.", request.getRequestURI()));
	}

	/** A path variable or query parameter could not be converted, e.g. /api/users/abc. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		return ResponseEntity.badRequest().body(ErrorResponse.of(
				HttpStatus.BAD_REQUEST.value(), "INVALID_PARAMETER",
				"Parameter '" + ex.getName() + "' has the wrong type.", request.getRequestURI(),
				List.of(new ErrorResponse.FieldError(ex.getName(), "wrong type"))));
	}

	/**
	 * No handler matched the URL. Without this the catch-all below would turn every
	 * stray request into a logged 500.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(
				HttpStatus.NOT_FOUND.value(), "NOT_FOUND",
				"No endpoint matches this path.", request.getRequestURI()));
	}

	/**
	 * A database constraint rejected the write. This is the race-condition path: two
	 * requests both pass an "is this email free?" check, and the unique index stops the
	 * second one. The message is deliberately generic, since the driver's text names
	 * tables, columns and constraints.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest request) {
		log.warn("Database constraint rejected a write on {}", request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(
				HttpStatus.CONFLICT.value(), "CONSTRAINT_VIOLATION",
				"The request conflicts with data that already exists.", request.getRequestURI()));
	}

	/** Thrown by a method-level {@code @PreAuthorize} that denied the call. */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse.of(
				HttpStatus.FORBIDDEN.value(), "FORBIDDEN",
				"You are not allowed to perform this action.", request.getRequestURI()));
	}

	/**
	 * Anything unforeseen. The stack trace is logged and never sent: leaking class
	 * names, SQL or file paths to a client is an information-disclosure bug, and the
	 * subject requires a clean browser console.
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse.of(
				HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
				"An unexpected error occurred.", request.getRequestURI()));
	}
}
