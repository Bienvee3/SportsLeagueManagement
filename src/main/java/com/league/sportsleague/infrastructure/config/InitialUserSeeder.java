package com.league.sportsleague.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.stereotype.Component;

/**
 * Creates the ADMIN, OFFICIAL and VIEWER users from ADMIN_* / OFFICIAL_* / VIEWER_* environment
 * variables on startup when absent. Fails fast if a username is set, the user does not exist yet
 * and its password is missing.
 */
@Component
public class InitialUserSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE    = "ADMIN";
    private static final String OFFICIAL_ROLE = "OFFICIAL";
    private static final String VIEWER_ROLE   = "VIEWER";

    private final UserDetailsManager userDetailsManager;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;
    private final String officialUsername;
    private final String officialPassword;
    private final String viewerUsername;
    private final String viewerPassword;

    public InitialUserSeeder(UserDetailsManager userDetailsManager,
                             PasswordEncoder passwordEncoder,
                             @Value("${ADMIN_USERNAME:}") String adminUsername,
                             @Value("${ADMIN_PASSWORD:}") String adminPassword,
                             @Value("${OFFICIAL_USERNAME:}") String officialUsername,
                             @Value("${OFFICIAL_PASSWORD:}") String officialPassword,
                             @Value("${VIEWER_USERNAME:}") String viewerUsername,
                             @Value("${VIEWER_PASSWORD:}") String viewerPassword) {
        this.userDetailsManager = userDetailsManager;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
        this.officialUsername = officialUsername;
        this.officialPassword = officialPassword;
        this.viewerUsername = viewerUsername;
        this.viewerPassword = viewerPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUser(adminUsername, adminPassword, ADMIN_ROLE);
        seedUser(officialUsername, officialPassword, OFFICIAL_ROLE);
        seedUser(viewerUsername, viewerPassword, VIEWER_ROLE);
    }

    private void seedUser(String username, String rawPassword, String role) {
        if (username.isBlank()) {
            return; // Not configured; an existing user from a prior run is sufficient.
        }
        if (userDetailsManager.userExists(username)) {
            return; // Never overwrite an existing account.
        }
        if (rawPassword.isBlank()) {
            throw new IllegalStateException(
                    "%s_PASSWORD environment variable must be set to create the %s user"
                            .formatted(role, role.toLowerCase()));
        }
        userDetailsManager.createUser(User.withUsername(username)
                .password(passwordEncoder.encode(rawPassword))
                .roles(role)
                .build());
    }
}
