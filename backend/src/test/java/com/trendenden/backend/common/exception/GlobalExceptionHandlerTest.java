package com.trendenden.backend.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Standalone MockMvc, so this exercises the advice alone without a database or a security
 * chain. The shape it produces is checked against the real converter in
 * {@code ErrorShapeConsistencyIT}.
 */
class GlobalExceptionHandlerTest {

	record Dto(@NotBlank String username, @Email String email) {
	}

	@RestController
	static class Probe {

		@GetMapping("/probe/notfound")
		void notFound() {
			throw new NotFoundException("USER_NOT_FOUND", "No user with id 42.");
		}

		@GetMapping("/probe/conflict")
		void conflict() {
			throw new ConflictException("EMAIL_ALREADY_USED", "That email is taken.");
		}

		@GetMapping("/probe/forbidden")
		void forbidden() {
			throw new ForbiddenException("NOT_YOURS", "Not your resource.");
		}

		@GetMapping("/probe/integrity")
		void integrity() {
			throw new DataIntegrityViolationException(
					"ERROR: duplicate key value violates unique constraint \"users_email_key\" "
							+ "Detail: Key (email)=(victim@example.com) already exists.");
		}

		@GetMapping("/probe/boom")
		void boom() {
			throw new IllegalStateException("internal detail /Users/ader/secrets.sql");
		}

		@GetMapping("/probe/typed/{id}")
		void typed(@PathVariable Long id) {
		}

		@PostMapping("/probe/validate")
		void validate(@Valid @RequestBody Dto dto) {
		}
	}

	private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new Probe())
			.setControllerAdvice(new GlobalExceptionHandler())
			.build();

	/** One handler covers all five subclasses, because it is registered on the base type. */
	@Test
	void everyApiExceptionKeepsItsOwnStatusAndCode() throws Exception {
		mvc.perform(get("/probe/notfound"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
				.andExpect(jsonPath("$.path").value("/probe/notfound"))
				.andExpect(jsonPath("$.timestamp").isString())
				// @JsonInclude(NON_NULL) keeps the unused half out of the body.
				.andExpect(jsonPath("$.fieldErrors").doesNotExist());

		mvc.perform(get("/probe/conflict")).andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
		mvc.perform(get("/probe/forbidden")).andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_YOURS"));
	}

	@Test
	void allInvalidFieldsAreReportedTogether() throws Exception {
		mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"username":"","email":"not-an-email"}"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
				.andExpect(jsonPath("$.fieldErrors.length()").value(2));
	}

	/** Jackson's own message quotes the input and names our classes. */
	@Test
	void malformedJsonIsFourHundredAndLeaksNothing() throws Exception {
		mvc.perform(post("/probe/validate").contentType(MediaType.APPLICATION_JSON)
						.content("{not json"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
				.andExpect(jsonPath("$.message")
						.value("Request body is missing or not valid JSON."));
	}

	/** Without this handler the catch-all would turn a client mistake into a 500. */
	@Test
	void aPathVariableOfTheWrongTypeIsFourHundred() throws Exception {
		mvc.perform(get("/probe/typed/abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("id"));
	}

	/**
	 * The race-condition path. The driver's text names the table, the column, the
	 * constraint, and another user's email, so none of it may be returned.
	 */
	@Test
	void aConstraintViolationIsConflictAndRevealsNoSchemaOrData() throws Exception {
		mvc.perform(get("/probe/integrity"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONSTRAINT_VIOLATION"))
				.andExpect(jsonPath("$.message")
						.value("The request conflicts with data that already exists."));
	}

	@Test
	void anUnexpectedErrorIsFiveHundredAndRevealsNoInternals() throws Exception {
		mvc.perform(get("/probe/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").value("An unexpected error occurred."));
	}
}
