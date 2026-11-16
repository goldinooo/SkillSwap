package com.trendenden.backend.auth.service;

import com.trendenden.backend.auth.entity.OAuthProvider;
import com.trendenden.backend.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.Instant;

/**
 * Runs after a provider has authenticated the user, and converts that into our own
 * tokens.
 *
 * <p>Spring Security's own default would start a session and redirect to a page. This
 * backend is stateless and the frontend is a separate origin, so instead the refresh token
 * is handed to the frontend in the redirect URL and exchanged there for a token pair.
 *
 * <p>Only the refresh token is passed, not the access token: the URL ends up in browser
 * history and in the referrer, so the value in it is immediately spent and rotated away by
 * the first {@code /api/auth/refresh} call.
 */
@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

	private static final Logger log =
			LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

	private final OAuthAccountService oauthAccountService;
	private final RefreshTokenService refreshTokenService;
	private final String frontendRedirectUri;

	public OAuth2LoginSuccessHandler(OAuthAccountService oauthAccountService,
			RefreshTokenService refreshTokenService,
			@Value("${app.oauth2.success-redirect-uri}") String frontendRedirectUri) {
		this.oauthAccountService = oauthAccountService;
		this.refreshTokenService = refreshTokenService;
		this.frontendRedirectUri = frontendRedirectUri;
	}

	@Override
	public void onAuthenticationSuccess(HttpServletRequest request,
			HttpServletResponse response, Authentication authentication) throws IOException {

		if (!(authentication instanceof OAuth2AuthenticationToken token)) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		OAuthProvider provider = OAuthProvider
				.fromRegistrationId(token.getAuthorizedClientRegistrationId())
				.orElse(null);
		if (provider == null) {
			log.warn("OAuth2 login from unknown registration '{}'",
					token.getAuthorizedClientRegistrationId());
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		OAuth2User principal = token.getPrincipal();
		String providerUserId = providerUserId(provider, principal);
		String email = attribute(principal, "email");

		// Both providers can be configured to withhold the address. Without it there is no
		// safe way to decide whether this is a new account or an existing one, so the
		// login is refused rather than guessed.
		if (providerUserId == null || email == null) {
			log.warn("{} login was missing an id or email attribute", provider);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		User user = oauthAccountService.resolve(
				provider, providerUserId, email, displayName(provider, principal));
		user.touchLastSeen(Instant.now());

		String refreshToken = refreshTokenService.issue(user);

		response.sendRedirect(UriComponentsBuilder.fromUriString(frontendRedirectUri)
				.queryParam("refreshToken", refreshToken)
				.build()
				.toUriString());
	}

	/** Google calls it {@code sub}; 42 calls it {@code id} and sends it as a number. */
	private String providerUserId(OAuthProvider provider, OAuth2User principal) {
		String key = provider == OAuthProvider.GOOGLE ? "sub" : "id";
		Object value = principal.getAttribute(key);
		return value == null ? null : String.valueOf(value);
	}

	private String displayName(OAuthProvider provider, OAuth2User principal) {
		String name = provider == OAuthProvider.GOOGLE
				? attribute(principal, "name")
				: attribute(principal, "displayname");
		return name != null ? name : attribute(principal, "login");
	}

	private String attribute(OAuth2User principal, String key) {
		Object value = principal.getAttribute(key);
		return value instanceof String text && !text.isBlank() ? text : null;
	}
}
