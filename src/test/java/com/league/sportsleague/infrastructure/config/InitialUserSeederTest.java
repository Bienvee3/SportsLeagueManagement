package com.league.sportsleague.infrastructure.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialUserSeederTest {

    @Mock UserDetailsManager userDetailsManager;
    @Mock PasswordEncoder passwordEncoder;

    private InitialUserSeeder seeder(String adminUser, String adminPass,
                                     String officialUser, String officialPass,
                                     String viewerUser, String viewerPass) {
        return new InitialUserSeeder(userDetailsManager, passwordEncoder,
                adminUser, adminPass, officialUser, officialPass, viewerUser, viewerPass);
    }

    private static Set<String> authorities(UserDetails user) {
        return user.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("run creates admin, official and viewer with exactly their own role when all are absent")
    void createsAllThreeUsersWhenAbsent() {
        when(userDetailsManager.userExists(any())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");

        seeder("admin", "a-secret", "official", "o-secret", "viewer", "v-secret").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager, times(3)).createUser(captor.capture());
        assertEquals(Set.of("ROLE_ADMIN"), authorities(userNamed(captor, "admin")));
        assertEquals(Set.of("ROLE_OFFICIAL"), authorities(userNamed(captor, "official")));
        assertEquals(Set.of("ROLE_VIEWER"), authorities(userNamed(captor, "viewer")));
        assertEquals("{bcrypt}hash", userNamed(captor, "viewer").getPassword());
    }

    @Test
    @DisplayName("run is idempotent: existing users are never recreated or modified")
    void doesNothingWhenAllUsersExist() {
        when(userDetailsManager.userExists(any())).thenReturn(true);

        seeder("admin", "a-secret", "official", "o-secret", "viewer", "v-secret").run(null);

        verify(userDetailsManager, never()).createUser(any());
        verify(userDetailsManager, never()).updateUser(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    @DisplayName("run fails fast when admin is absent and its password is blank")
    void failsFastWhenAdminPasswordMissing() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);

        assertThrows(IllegalStateException.class,
                () -> seeder("admin", "", "", "", "", "").run(null));
    }

    @Test
    @DisplayName("run fails fast naming OFFICIAL_PASSWORD when official is absent and its password is blank")
    void failsFastWhenOfficialPasswordMissing() {
        when(userDetailsManager.userExists("admin")).thenReturn(true);
        when(userDetailsManager.userExists("official")).thenReturn(false);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> seeder("admin", "a-secret", "official", "", "viewer", "v-secret").run(null));
        assertTrue(ex.getMessage().contains("OFFICIAL_PASSWORD"), ex.getMessage());
    }

    @Test
    @DisplayName("run skips roles whose username is not configured")
    void skipsRolesWithoutUsername() {
        when(userDetailsManager.userExists("admin")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("{bcrypt}hash");

        seeder("admin", "a-secret", "", "", "", "").run(null);

        ArgumentCaptor<UserDetails> captor = ArgumentCaptor.forClass(UserDetails.class);
        verify(userDetailsManager, times(1)).createUser(captor.capture());
        assertEquals("admin", captor.getValue().getUsername());
        verify(userDetailsManager, never()).userExists(eq(""));
    }

    private static UserDetails userNamed(ArgumentCaptor<UserDetails> captor, String username) {
        return captor.getAllValues().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElseThrow(() -> new AssertionError(username + " not created"));
    }
}
