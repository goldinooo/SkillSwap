package com.trendenden.backend.publicapi.controller;

import com.trendenden.backend.common.security.CurrentUser;
import com.trendenden.backend.publicapi.dto.PlatformStatsResponse;
import com.trendenden.backend.user.dto.PublicUserResponse;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import com.trendenden.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * The Public API: authenticated with {@code X-API-Key} and rate limited per key.
 *
 * <p>Deliberately not {@code @Validated}: that annotation proxies the class and routes
 * parameter constraints through {@code MethodValidationInterceptor}, which throws
 * {@code jakarta.validation.ConstraintViolationException}. Spring MVC's own method
 * validation throws {@code HandlerMethodValidationException} instead, which is the one
 * that carries per-parameter detail into {@code ErrorResponse.fieldErrors}. Leaving it off
 * keeps a single validation path for web parameters.
 *
 * <p>Everything exposed here reads only the tables V10 owns. Endpoints for skills,
 * classes and the leaderboard belong to the Public API too but need V20/V21, so they are
 * added once those migrations land.
 */
@RestController
@RequestMapping("/api/v1/public")
@SecurityRequirement(name = "apiKeyAuth")
public class PublicApiController {

	/** Capped so one call cannot ask for the entire user table. */
	private static final int MAX_PAGE_SIZE = 100;

	private final UserService userService;
	private final UserRepository userRepository;
	private final CurrentUser currentUser;

	public PublicApiController(UserService userService, UserRepository userRepository,
			CurrentUser currentUser) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.currentUser = currentUser;
	}

	/** 1. Liveness. The one endpoint that needs no key, so a client can check reachability. */
	@GetMapping("/health")
	@SecurityRequirements
	@Operation(summary = "API status")
	public Map<String, Object> health() {
		return Map.of("status", "ok", "version", "v1", "time", Instant.now());
	}

	/**
	 * 2. The user directory: filtering, sorting and pagination, which is what the Advanced
	 * search module is assessed on.
	 */
	@GetMapping("/users")
	@Operation(summary = "Search users, paginated")
	public PageResponse<PublicUserResponse> users(
			@RequestParam(required = false) @Size(max = 100) String q,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size,
			@Parameter(description = "createdAt, username or displayName")
			@RequestParam(defaultValue = "createdAt") String sort,
			@RequestParam(defaultValue = "desc") String direction) {

		Sort.Direction sortDirection = "asc".equalsIgnoreCase(direction)
				? Sort.Direction.ASC : Sort.Direction.DESC;

		// Allow-listed rather than passed straight through: an arbitrary sort string
		// reaches the JPA metamodel and would let a caller order by password_hash, which
		// leaks information about values it must never see.
		String sortProperty = switch (sort) {
			case "username" -> "username";
			case "displayName" -> "displayName";
			default -> "createdAt";
		};

		Page<User> found = userService.findPublic(q,
				PageRequest.of(page, size, Sort.by(sortDirection, sortProperty)));

		return PageResponse.from(found.map(PublicUserResponse::from));
	}

	/** 3. One public profile, addressed by the stable, human-readable username. */
	@GetMapping("/users/{username}")
	@Operation(summary = "A single user's public profile")
	public PublicUserResponse user(@PathVariable String username) {
		return PublicUserResponse.from(userService.getByUsername(username));
	}

	/** 4. Aggregate counts only. Nothing here identifies an individual. */
	@GetMapping("/stats")
	@Operation(summary = "Platform statistics")
	public PlatformStatsResponse stats() {
		return new PlatformStatsResponse(userRepository.count(), Instant.now());
	}

	/** 5. Who the calling key belongs to: lets a client confirm which account it is acting as. */
	@GetMapping("/me")
	@Operation(summary = "The account this API key belongs to")
	public PublicUserResponse me() {
		return PublicUserResponse.from(userService.getById(currentUser.currentUserId()));
	}

	/**
	 * Spring Data's {@code Page} serialises its internal structure and warns about it.
	 * This is the stable payload the API promises instead.
	 */
	public record PageResponse<T>(
			List<T> content, int page, int size, long totalElements, int totalPages,
			boolean first, boolean last
	) {
		static <T> PageResponse<T> from(Page<T> page) {
			return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
					page.getTotalElements(), page.getTotalPages(),
					page.isFirst(), page.isLast());
		}
	}
}
