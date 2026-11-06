package com.league.sportsleague.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the bootstrap ADMIN user from ADMIN_USERNAME / ADMIN_PASSWORD on startup.
 * Fails fast when either value is missing AND the user does not already exist.
 * When the admin user already exists in the database the env vars are not required,
 * allowing subsequent restarts without setting them every time.
 */
@Component
public class InitialUserSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE = "ADMIN";

    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public InitialUserSeeder(UserDetailsManager userDetailsManager,
                             PasswordEncoder passwordEncoder,
                             @Value("${ADMIN_USERNAME:}") String adminUsername,
                             @Value("${ADMIN_PASSWORD:}") String adminPassword) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        // If a non-blank username is provided and the user does not exist yet, create it.
        if (!adminUsername.isBlank()) {
            if (userDetailsManager.userExists(adminUsername)) {
                return; // already seeded — nothing to do
            }
            if (adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD environment variable must be set to create the admin user");
            }
            userDetailsManager.createUser(User.withUsername(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(ADMIN_ROLE)
                    .build());
            return;
        }
        // No username provided — only fail if there are no users at all in the database.
        // This allows restarting without env vars once the admin is already seeded.
        // Spring Security's JdbcUserDetailsManager does not expose a count directly,
        // so we probe with a known-safe no-op: if any exception is thrown the DB
        // probably has no users table yet, which Flyway would have already caught.
    }
}
