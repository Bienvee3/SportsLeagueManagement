package com.league.sportsleague.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates the bootstrap ADMIN user from ADMIN_USERNAME / ADMIN_PASSWORD on startup.
 * Also seeds test-only dev accounts (test-official, test-viewer) for role preview.
 * Fails fast when ADMIN_USERNAME is set but ADMIN_PASSWORD is missing AND the admin
 * user does not already exist.
 */
@Component
public class InitialUserSeeder implements ApplicationRunner {

    private static final String ADMIN_ROLE      = "ADMIN";
    private static final String OFFICIAL_ROLE   = "OFFICIAL";
    private static final String VIEWER_ROLE     = "VIEWER";
    /** Grants impersonation capability via Spring Security's SwitchUserFilter. */
    private static final String SWITCH_ROLE     = "SWITCH_USER";

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
        seedAdmin();
        seedDevUser("test-official", "official123", OFFICIAL_ROLE);
        seedDevUser("test-viewer",   "viewer123",   VIEWER_ROLE);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void seedAdmin() {
        if (adminUsername.isBlank()) {
            return; // No username provided; existing admin from prior run is sufficient.
        }
        if (!userDetailsManager.userExists(adminUsername)) {
            if (adminPassword.isBlank()) {
                throw new IllegalStateException(
                        "ADMIN_PASSWORD environment variable must be set to create the admin user");
            }
            userDetailsManager.createUser(User.withUsername(adminUsername)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(ADMIN_ROLE, SWITCH_ROLE)
                    .build());
            return;
        }
        // Admin already exists — ensure ROLE_SWITCH_USER is present (idempotent upgrade).
        UserDetails existing = userDetailsManager.loadUserByUsername(adminUsername);
        boolean hasSwitchRole = existing.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + SWITCH_ROLE));
        if (!hasSwitchRole) {
            List<GrantedAuthority> authorities = new ArrayList<>(existing.getAuthorities());
            authorities.add(new SimpleGrantedAuthority("ROLE_" + SWITCH_ROLE));
            UserDetails updated = User.withUserDetails(existing)
                    .authorities(authorities)
                    .build();
            userDetailsManager.updateUser(updated);
        }
    }

    private void seedDevUser(String username, String rawPassword, String role) {
        if (!userDetailsManager.userExists(username)) {
            userDetailsManager.createUser(User.withUsername(username)
                    .password(passwordEncoder.encode(rawPassword))
                    .roles(role)
                    .build());
        }
    }
}
