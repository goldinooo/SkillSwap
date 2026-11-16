package com.trendenden.backend.publicapi.security;

import com.trendenden.backend.common.exception.ErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-key rate limiting for the Public API, on Bucket4j's core token-bucket API.
 *
 * <p>Written as a plain filter rather than through {@code bucket4j-spring-boot-starter}:
 * that starter refuses to start without a cache provider, which would mean another
 * dependency in the shared {@code pom.xml}, and its 0.14.0 release targets Spring Boot 3.
 * The core library is already on the classpath as a transitive dependency of it.
 *
 * <p>Buckets are keyed by API key id, not by IP. One client behind a shared address must
 * not be able to exhaust another's quota, and a single client cannot buy more quota by
 * rotating addresses.
 *
 * <p><b>Limitation.</b> The buckets live in this instance's heap, so running two instances
 * doubles the effective limit. Making the quota exact across instances needs shared
 * storage (Redis or Hazelcast), which is what the starter's cache requirement was for.
 * Documented rather than hidden, since it changes nothing for a single-instance deployment.
 */
public class RateLimitFilter extends OncePerRequestFilter {

	private final Map<Object, Bucket> buckets = new ConcurrentHashMap<>();

	private final ObjectMapper objectMapper;
	private final long requestsPerMinute;

	public RateLimitFilter(ObjectMapper objectMapper, long requestsPerMinute) {
		this.objectMapper = objectMapper;
		this.requestsPerMinute = requestsPerMinute;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		Object keyId = request.getAttribute(ApiKeyAuthFilter.KEY_ID_ATTRIBUTE);

		// No authenticated key: nothing to meter, and the chain's own rules will reject
		// the request with a 401 in a moment.
		if (keyId == null) {
			filterChain.doFilter(request, response);
			return;
		}

		Bucket bucket = buckets.computeIfAbsent(keyId, id -> Bucket.builder()
				.addLimit(Bandwidth.simple(requestsPerMinute, Duration.ofMinutes(1)))
				.build());

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

		// Sent on success as well as on rejection: a client can only pace itself if it
		// can see its remaining quota before it runs out.
		response.setHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
		response.setHeader("X-RateLimit-Remaining",
				String.valueOf(Math.max(0, probe.getRemainingTokens())));

		if (probe.isConsumed()) {
			filterChain.doFilter(request, response);
			return;
		}

		long retryAfterSeconds = Math.max(1,
				Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds());

		// Retry-After is the standard companion to 429. Without it a client has no way to
		// know how long to back off and will usually hammer the endpoint instead.
		response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
		response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
				HttpStatus.TOO_MANY_REQUESTS.value(), "RATE_LIMIT_EXCEEDED",
				"Too many requests. Retry in " + retryAfterSeconds + "s.",
				request.getRequestURI()));
	}
}
