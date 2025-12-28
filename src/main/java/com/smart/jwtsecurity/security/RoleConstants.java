package com.smart.jwtsecurity.security;

/**
 * Centralized role definitions.
 *
 * Roles are SECURITY POLICY, not data.
 * They must never be created or modified from DB.
 */
public final class RoleConstants {

    private RoleConstants() {}

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER  = "ROLE_USER";

    /**
     * Optional: validate role correctness
     */
    public static boolean isValid(String role) {
        return ROLE_ADMIN.equals(role) || ROLE_USER.equals(role);
    }
}
