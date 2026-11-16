package com.trendenden.backend.user.controller;

import com.trendenden.backend.common.security.CurrentUser;
import com.trendenden.backend.user.dto.PublicUserResponse;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.dto.UpdateProfileRequest;
import com.trendenden.backend.user.dto.UserResponse;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

	private final UserService userService;
	private final CurrentUser currentUser;

	public UserController(UserService userService, CurrentUser currentUser) {
		this.userService = userService;
		this.currentUser = currentUser;
	}

	/**
	 * The only open endpoint here. SecurityConfig permits POST on exactly this path, not
	 * {@code /api/users/**}, so every other method below still needs a token.
	 */
	@PostMapping
	@SecurityRequirements
	@Operation(summary = "Register with email and password")
	public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
		User user = userService.register(request);
		return ResponseEntity
				.created(UriComponentsBuilder.fromPath("/api/users/{id}")
						.buildAndExpand(user.getId()).toUri())
				.body(UserResponse.from(user));
	}

	@GetMapping("/me")
	@Operation(summary = "The signed-in user's own account")
	public UserResponse me() {
		return UserResponse.from(userService.getById(currentUser.currentUserId()));
	}

	@PatchMapping("/me")
	@Operation(summary = "Update the signed-in user's profile")
	public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest request) {
		return UserResponse.from(
				userService.updateProfile(currentUser.currentUserId(), request));
	}

	/**
	 * Someone else's profile, so it answers with the public projection even when the
	 * caller asks for their own id. {@code /me} is the endpoint for the full view.
	 */
	@GetMapping("/{id}")
	@Operation(summary = "Another user's public profile")
	public PublicUserResponse byId(@PathVariable UUID id) {
		return PublicUserResponse.from(userService.getById(id));
	}

	/**
	 * Used by the signup form before the user has a token, so SecurityConfig permits it.
	 * On its own path rather than {@code GET /api/users?username=}, which would collide
	 * with a future listing endpoint and could not be opened without opening that too.
	 */
	@GetMapping("/availability")
	@SecurityRequirements
	@Operation(summary = "Check whether a username is free")
	public ResponseEntity<Void> usernameAvailability(@RequestParam String username) {
		return userService.isUsernameAvailable(username)
				? ResponseEntity.noContent().build()
				: ResponseEntity.status(HttpStatus.CONFLICT).build();
	}
}
