package com.trendenden.backend.common.security;

import tools.jackson.databind.ObjectMapper;
import com.trendenden.backend.common.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Produces the 401 and 403 that Spring Security's filter chain raises.
 *
 * <p>This class exists because {@code GlobalExceptionHandler} cannot cover these. The
 * security filters run before the DispatcherServlet, so a missing or invalid token never
 * reaches {@code @RestControllerAdvice}. Without this handler Spring would answer with its
 * own default body, and the two most common error responses in the whole API would have a
 * different shape from every other one.
 *
 * <p>Both interfaces are implemented by one class so the two bodies cannot drift apart.
 */
@Component
public class RestAuthErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final ObjectMapper objectMapper;

	/**
	 * Takes the Boot-configured mapper on purpose: a hand-built one is not set up to write
	 * {@code Instant} as an ISO-8601 string.
	 *
	 * <p>Note the package: Spring Boot 4 ships Jackson 3, whose mapper is
	 * {@code tools.jackson.databind.ObjectMapper}. The Jackson 2 class of the same name
	 * ({@code com.fasterxml.jackson.databind.ObjectMapper}) is still on the classpath as a
	 * transitive dependency of springdoc, so importing it compiles and then fails at
	 * startup with "No qualifying bean of type ObjectMapper". Annotations are the
	 * exception: Jackson 3 still reads {@code com.fasterxml.jackson.annotation.*}, which is
	 * why {@code ErrorResponse} keeps that import for {@code @JsonInclude}.
	 */
	public RestAuthErrorHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/** No credentials, or credentials that did not verify. */
	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
				"Authentication is required.");
	}

	/** Valid credentials, insufficient authority. */
	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN",
				"You are not allowed to perform this action.");
	}

	private void write(HttpServletRequest request, HttpServletResponse response,
			HttpStatus status, String code, String message) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
				status.value(), code, message, request.getRequestURI()));
	}
}
