package com.smart.jwtsecurity.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.smart.jwtsecurity.filter.JwtAuthorizationFilter;
import com.smart.jwtsecurity.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final JwtUtil jwtUtil;
	private final AuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final AccessDeniedHandler jwtAccessDeniedHandler;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

		http
				// =========================
				// ❌ Disable CSRF (stateless API)
				// =========================
				.csrf(csrf -> csrf.disable())

				// =========================
				// ❌ No HttpSession (JWT only)
				// =========================
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

				// =========================
				// 🔐 Authorization rules
				// =========================
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/login", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()

						.requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())

				// =========================
				// ❌ Disable form login
				// =========================
				.formLogin(form -> form.disable())

				// =========================
				// ✅ Enable HTTP Basic ONLY for login
				// =========================
				.httpBasic(basic -> basic.authenticationEntryPoint(jwtAuthenticationEntryPoint))

				// =========================
				// 🧪 Custom 401 / 403 handlers
				// =========================
				.exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
						.accessDeniedHandler(jwtAccessDeniedHandler));

		// =========================
		// 🔑 JWT Authorization Filter
		// =========================
		http.addFilterBefore(new JwtAuthorizationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	// =========================
	// 🔐 BCrypt Password Encoder
	// =========================
	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	// =========================
	// 🔑 AuthenticationManager
	// =========================
	@Bean
	AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}
}
