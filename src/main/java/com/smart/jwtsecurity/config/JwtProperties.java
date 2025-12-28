package com.smart.jwtsecurity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Strongly typed JWT configuration.
 *
 * Why this class exists: - Externalizes security-sensitive config - Prevents
 * hard-coded secrets - Enables validation at startup (fail-fast)
 *
 * Loaded automatically by Spring Boot.
 */
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

	/**
	 * HMAC secret key. MUST be at least 256 bits (32+ characters) for HS256.
	 */
	@NotBlank
	private String secret;

	/**
	 * Token issuer. Used to prevent token spoofing across services.
	 */
	@NotBlank
	private String issuer;

	/**
	 * Access token expiration time in milliseconds. Example: 900000 (15 minutes)
	 */
	@Min(60000) // minimum 1 minute
	private long expirationMs;
}
