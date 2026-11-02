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
 * Fails fast when either value is missing; does nothing when the user already exists.
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
        if (adminUsername == null || adminUsername.isBlank()) {
            throw new IllegalStateException("ADMIN_USERNAME environment variable must be set");
        }
        if (adminPassword == null || adminPassword.isBlank()) {
            throw new IllegalStateException("ADMIN_PASSWORD environment variable must be set");
        }
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (userDetailsManager.userExists(adminUsername)) {
            return;
        }
        userDetailsManager.createUser(User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles(ADMIN_ROLE)
                .build());
    }
}
