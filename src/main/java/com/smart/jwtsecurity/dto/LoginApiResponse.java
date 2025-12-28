package com.smart.jwtsecurity.dto;

import java.util.Set;

import lombok.Builder;
import lombok.Getter;

/**
 * Response returned after successful login.
 *
 * This is a PUBLIC API contract. Keep it stable and explicit.
 */
@Getter
@Builder
public class LoginApiResponse {

	/**
	 * JWT access token.
	 */
	private final String accessToken;

	/**
	 * Token type (always Bearer).
	 */
	private final String tokenType;

	/**
	 * Token expiration time in seconds.
	 */
	private final long expiresInSeconds;

	/**
	 * Authenticated username.
	 */
	private final String username;

	/**
	 * Roles granted to the user.
	 */
	private final Set<String> roles;
}
