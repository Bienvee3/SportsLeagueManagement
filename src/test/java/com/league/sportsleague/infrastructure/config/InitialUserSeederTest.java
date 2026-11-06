package com.league.sportsleague.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialUserSeederTest {

    @Mock UserDetailsManager userDetailsManager;
    @Mock PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("run creates admin with encoded password and ROLE_ADMIN when env vars provided and user absent")
    void createsAdminWhenAbsent() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("{bcrypt}hash");

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "secret").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager).createUser(captor.capture());
        UserDetails created = captor.getValue();
        assertEquals("admin", created.getUsername());
        assertEquals("{bcrypt}hash", created.getPassword());
        assertTrue(created.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("run is idempotent: does not recreate an existing admin")
    void doesNothingWhenAdminExists() {
        when(userDetailsManager.userExists("admin")).thenReturn(true);

        new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "secret").run(null);

        verify(userDetailsManager, never()).createUser(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("run fails fast when username provided but password is blank")
    void failsFastWhenPasswordMissingAndUserAbsent() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> new InitialUserSeeder(userDetailsManager, passwordEncoder, "admin", "").run(null));
    }

    @Test
    @DisplayName("run does nothing when no env vars provided — allows restart without vars")
    void doesNothingWhenNoEnvVarsProvided() {
        new InitialUserSeeder(userDetailsManager, passwordEncoder, "", "").run(null);

        verifyNoInteractions(userDetailsManager);
        verifyNoInteractions(passwordEncoder);
    }
}
