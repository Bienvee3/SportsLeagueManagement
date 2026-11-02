package com.league.sportsleague.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityAccessTest {

    @Autowired MockMvc mockMvc;

    @Test
    @DisplayName("anonymous request to a protected page redirects to /login")
    void anonymousIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/leagues"))
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("login page is publicly reachable")
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can reach general pages")
    void viewerCanReachGeneralPages() throws Exception {
        mockMvc.perform(get("/leagues"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER is forbidden from /admin/**")
    void viewerIsForbiddenFromAdmin() throws Exception {
        mockMvc.perform(get("/admin/leagues"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER is forbidden from /official/**")
    void viewerIsForbiddenFromOfficial() throws Exception {
        mockMvc.perform(get("/official/fixtures"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("OFFICIAL can reach /official/** but not /admin/**")
    void officialScopes() throws Exception {
        mockMvc.perform(get("/official/fixtures"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/admin/leagues"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("ADMIN can reach /admin/** and /official/**")
    void adminScopes() throws Exception {
        mockMvc.perform(get("/admin/leagues"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/official/fixtures"))
                .andExpect(status().isNotFound());
    }
}
