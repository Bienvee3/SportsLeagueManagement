package com.league.sportsleague.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialUserSeederTest {

    @Mock UserDetailsManager userDetailsManager;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("run creates admin + dev users with correct roles when env vars provided and admin absent")
    void createsAdminWhenAbsent() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);
        when(userDetailsManager.userExists("test-official")).thenReturn(false);
        when(userDetailsManager.userExists("test-viewer")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "secret").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager, times(3)).createUser(captor.capture());

        // Find the admin user and verify its authorities
        UserDetails adminUser = captor.getAllValues().stream()
                .filter(u -> u.getUsername().equals("admin"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("admin not created"));
        assertEquals("{bcrypt}hash", adminUser.getPassword());
        assertTrue(adminUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertTrue(adminUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SWITCH_USER")),
                "Admin must have ROLE_SWITCH_USER for dev role preview");
    }

    @Test
    @DisplayName("run is idempotent: does not recreate existing users when all already present")
    void doesNothingWhenAllUsersExist() {
        UserDetails adminDetails = User.withUsername("admin")
                .password("{bcrypt}hash")
                .roles("ADMIN", "SWITCH_USER")
                .build();
        when(userDetailsManager.userExists("admin")).thenReturn(true);
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(adminDetails);
        when(userDetailsManager.userExists("test-official")).thenReturn(true);
        when(userDetailsManager.userExists("test-viewer")).thenReturn(true);

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "secret").run(null);

        verify(userDetailsManager, never()).createUser(any());
        verify(userDetailsManager, never()).updateUser(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("run adds ROLE_SWITCH_USER to existing admin missing that role")
    void upgradesExistingAdminWithSwitchRole() {
        UserDetails adminWithoutSwitch = User.withUsername("admin")
                .password("{bcrypt}hash")
                .roles("ADMIN")           // no SWITCH_USER
                .build();
        when(userDetailsManager.userExists("admin")).thenReturn(true);
        when(userDetailsManager.loadUserByUsername("admin")).thenReturn(adminWithoutSwitch);
        when(userDetailsManager.userExists("test-official")).thenReturn(true);
        when(userDetailsManager.userExists("test-viewer")).thenReturn(true);

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "secret").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager).updateUser(captor.capture());
        assertTrue(captor.getValue().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SWITCH_USER")));
    }

    @Test
    @DisplayName("run fails fast when username provided but password is blank")
    void failsFastWhenPasswordMissingAndUserAbsent() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "").run(null));
    }

    @Test
    @DisplayName("run seeds only dev users when no admin env vars provided")
    void seedsDevUsersWhenNoAdminEnvVars() {
        when(userDetailsManager.userExists("test-official")).thenReturn(false);
        when(userDetailsManager.userExists("test-viewer")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "", "").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager, times(2)).createUser(captor.capture());
        List<String> usernames = captor.getAllValues().stream()
                .map(UserDetails::getUsername)
                .toList();
        assertTrue(usernames.contains("test-official"));
        assertTrue(usernames.contains("test-viewer"));
    }
}
