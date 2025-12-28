-- ===============================
-- USERS TABLE
-- ===============================
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    username VARCHAR(100) NOT NULL,
    email    VARCHAR(150) NOT NULL,
    password VARCHAR(255) NOT NULL,

    enabled  BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email    UNIQUE (email)
);

-- ===============================
-- USER_ROLES TABLE
-- ===============================
-- Stores ONLY role assignments.
-- Roles themselves are defined in application code.
-- Example values:
--   ROLE_ADMIN
--   ROLE_USER
-- ===============================
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL,
    role    VARCHAR(50) NOT NULL,

    PRIMARY KEY (user_id, role),

    CONSTRAINT fk_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
        ON DELETE CASCADE
);
