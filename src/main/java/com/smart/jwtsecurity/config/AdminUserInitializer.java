package com.smart.jwtsecurity.config;

import java.util.HashSet;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smart.jwtsecurity.domain.entity.User;
import com.smart.jwtsecurity.repository.UserRepository;
import com.smart.jwtsecurity.security.RoleConstants;

import lombok.RequiredArgsConstructor;

/**
 * Ensures default ADMIN user exists.
 *
 * Roles come ONLY from RoleConstants. No roles are created in DB.
 */
@Component
@RequiredArgsConstructor
public class AdminUserInitializer implements ApplicationRunner {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	private static final String ADMIN_USERNAME = "admin";
	private static final String ADMIN_EMAIL = "admin@system.local";
	private static final String ADMIN_PASSWORD = "admin123";

	@Override
	@Transactional
	public void run(ApplicationArguments args) {

		User admin = userRepository.findByUsernameOrEmail(ADMIN_USERNAME, ADMIN_EMAIL).orElseGet(this::createAdminUser);

		boolean updated = false;

		if (!admin.getRoles().contains(RoleConstants.ROLE_ADMIN)) {
			admin.getRoles().add(RoleConstants.ROLE_ADMIN);
			updated = true;
		}

		if (!admin.getRoles().contains(RoleConstants.ROLE_USER)) {
			admin.getRoles().add(RoleConstants.ROLE_USER);
			updated = true;
		}

		if (updated) {
			userRepository.save(admin);
		}
	}

	private User createAdminUser() {
		return userRepository.save(User.builder().username(ADMIN_USERNAME).email(ADMIN_EMAIL)
				.password(passwordEncoder.encode(ADMIN_PASSWORD)).enabled(true).roles(new HashSet<>()).build());
	}
}
