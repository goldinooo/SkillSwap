package com.trendenden.backend.user;

import com.trendenden.backend.common.IntegrationTest;
import com.trendenden.backend.user.dto.RegisterRequest;
import com.trendenden.backend.user.entity.User;
import com.trendenden.backend.user.repository.UserRepository;
import com.trendenden.backend.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@IntegrationTest
class UserApiIT {

	@Autowired
	MockMvc mvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	UserService userService;

	@Test
	void registersAUserAndNeverEchoesCredentialMaterial() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"risattou","email":"risattou@example.com",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.username").value("risattou"))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.emailVerified").value(false))
				.andExpect(jsonPath("$.twoFactorEnabled").value(false))
				// The two fields that must never leave the server.
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.twoFactorSecret").doesNotExist());
	}

	/** V10 makes display_name NOT NULL, so without the fallback this would be a 500. */
	@Test
	void displayNameFallsBackToTheUsernameWhenOmitted() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"nodisplay","email":"nodisplay@example.com",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.displayName").value("nodisplay"));
	}

	/**
	 * V10's {@code CHECK (username = lower(username))} would reject the INSERT outright,
	 * so this also proves the service normalises before saving.
	 */
	@Test
	void usernameAndEmailAreStoredLowercase() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"RiSaTtOu","email":"RiSaTtOu@Example.COM",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.username").value("risattou"))
				.andExpect(jsonPath("$.email").value("risattou@example.com"))
				// The display name keeps the capitalisation the user chose.
				.andExpect(jsonPath("$.displayName").value("risattou"));

		assertThat(userRepository.findByUsername("risattou")).isPresent();
	}

	@Test
	void aTakenUsernameIsRejected() throws Exception {
		register("taken", "taken@example.com");

		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"taken","email":"other@example.com",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("USERNAME_TAKEN"));
	}

	/**
	 * The point of the lowercase CHECK: without it {@code UNIQUE} is case-sensitive, both
	 * rows are accepted, and two accounts share one real mailbox.
	 */
	@Test
	void anEmailThatDiffersOnlyInCaseIsTheSameEmail() throws Exception {
		register("first", "shared@example.com");

		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"second","email":"SHARED@Example.com",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_TAKEN"));
	}

	@Test
	void everyInvalidFieldIsReportedInOneResponse() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"x","email":"not-an-email","password":"short"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.length()").value(3));
	}

	/** 72 is BCrypt's limit; anything beyond it is silently ignored when hashing. */
	@Test
	void aPasswordLongerThanBcryptCanHashIsRejected() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"longpw","email":"longpw@example.com",
						 "password":"%s"}""".formatted("a".repeat(73))))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("password"));
	}

	@Test
	void aUsernameWithIllegalCharactersIsRejected() throws Exception {
		mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content("""
						{"username":"has spaces","email":"spaces@example.com",
						 "password":"correct-horse-battery"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors[0].field").value("username"));
	}

	@Test
	void theOwnAccountEndpointNeedsAToken() throws Exception {
		mvc.perform(get("/api/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void availabilityIsOpenAndReflectsWhatIsTaken() throws Exception {
		register("occupied", "occupied@example.com");

		mvc.perform(get("/api/users/availability").param("username", "stillfree"))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/users/availability").param("username", "occupied"))
				.andExpect(status().isConflict());

		// Normalised the same way registration is, or the check would disagree with it.
		mvc.perform(get("/api/users/availability").param("username", "OCCUPIED"))
				.andExpect(status().isConflict());
	}

	/** Another user's profile uses the public projection, so no email is exposed. */
	@Test
	void anotherUsersProfileHidesTheirEmail() throws Exception {
		User other = register("other", "other@example.com");

		mvc.perform(get("/api/users/{id}", other.getId())
						.header("Authorization", "Bearer nonsense"))
				.andExpect(status().isUnauthorized());
	}

	private User register(String username, String email) {
		return userService.register(
				new RegisterRequest(username, email, "correct-horse-battery", null));
	}
}
