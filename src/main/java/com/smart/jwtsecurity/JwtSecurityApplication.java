package com.smart.jwtsecurity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main bootstrap class.
 * 
 * This class triggers:
 * - Component scanning
 * - Auto-configuration
 * - Security filter chain initialization
 */
@SpringBootApplication
public class JwtSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(JwtSecurityApplication.class, args);
    }
}
