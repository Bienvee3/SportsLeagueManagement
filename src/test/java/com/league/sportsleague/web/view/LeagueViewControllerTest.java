package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.LeagueResponse;
import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeagueViewController.class)
@Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class LeagueViewControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LeagueService leagueService;
    @MockitoBean SeasonService seasonService;
    @MockitoBean DataSource dataSource;

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private LeagueResponse sample() {
        return new LeagueResponse(ID, "SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia", Instant.now());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /leagues returns 200 with leagues model")
    void listLeagues() throws Exception {
        when(leagueService.findAll()).thenReturn(List.of(sample()));
        mockMvc.perform(get("/leagues"))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/list"))
                .andExpect(model().attributeExists("leagues"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /leagues/{id} returns 200 with league model")
    void detailLeague() throws Exception {
        when(leagueService.findById(ID)).thenReturn(sample());
        when(seasonService.findByLeague(ID)).thenReturn(List.of());
        mockMvc.perform(get("/leagues/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/detail"))
                .andExpect(model().attributeExists("league"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /leagues/{id} returns 404 when not found")
    void detailNotFound() throws Exception {
        when(leagueService.findById(ID)).thenThrow(new EntityNotFoundException("League", ID));
        mockMvc.perform(get("/leagues/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /leagues/new returns 200 for ADMIN")
    void newFormAdmin() throws Exception {
        mockMvc.perform(get("/leagues/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/form"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /leagues/new returns 403 for VIEWER")
    void newFormViewer() throws Exception {
        mockMvc.perform(get("/leagues/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /leagues with blank name returns form with errors")
    void createValidationFails() throws Exception {
        mockMvc.perform(post("/leagues").with(csrf())
                        .param("name", "")
                        .param("slug", "test")
                        .param("country", "Croatia")
                        .param("sportType", "FOOTBALL"))
                .andExpect(status().isOk())
                .andExpect(view().name("leagues/form"));
    }

    @Test
    @DisplayName("GET /leagues returns 401 for anonymous (form-login redirect tested in SecurityAccessTest)")
    void anonymousRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/leagues"))
                .andExpect(status().isUnauthorized());
    }
}
