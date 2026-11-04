package com.league.sportsleague.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeasonRestController.class)
@org.springframework.context.annotation.Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class SeasonRestControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean SeasonService seasonService;

    private static final UUID LEAGUE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID SEASON_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private SeasonResponse sample() {
        return new SeasonResponse(SEASON_ID, LEAGUE_ID, "Test League", "test-league", "2026/2027", 2026, 2027, "UPCOMING");
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues/{id}/seasons returns 200 with list")
    void listByLeague() throws Exception {
        when(seasonService.findByLeague(LEAGUE_ID)).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons", LEAGUE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("2026/2027"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues/{id}/seasons/{sid} returns 200")
    void getById() throws Exception {
        when(seasonService.findById(SEASON_ID)).thenReturn(sample());
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/{id}", LEAGUE_ID, SEASON_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPCOMING"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET seasons/{id} returns 404 ProblemDetail when not found")
    void getByIdNotFound() throws Exception {
        when(seasonService.findById(SEASON_ID)).thenThrow(new EntityNotFoundException("Season", SEASON_ID));
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons/{id}", LEAGUE_ID, SEASON_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/leagues/{id}/seasons creates season and returns 201")
    void create() throws Exception {
        when(seasonService.createSeason(eq(LEAGUE_ID), any())).thenReturn(SEASON_ID);
        var request = new CreateSeasonRequest("2026/2027", 2026, 2027);
        mockMvc.perform(post("/api/leagues/{leagueId}/seasons", LEAGUE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(SEASON_ID.toString())));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/leagues/{id}/seasons returns 403 for VIEWER")
    void createForbiddenForViewer() throws Exception {
        var request = new CreateSeasonRequest("2026/2027", 2026, 2027);
        mockMvc.perform(post("/api/leagues/{leagueId}/seasons", LEAGUE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST activate returns 204")
    void activate() throws Exception {
        mockMvc.perform(post("/api/leagues/{leagueId}/seasons/{id}/activate", LEAGUE_ID, SEASON_ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST activate returns 409 ProblemDetail on invalid state")
    void activateInvalidState() throws Exception {
        doThrow(new InvalidStateTransitionException("Season", "ACTIVE", "ACTIVE"))
                .when(seasonService).activateSeason(SEASON_ID);
        mockMvc.perform(post("/api/leagues/{leagueId}/seasons/{id}/activate", LEAGUE_ID, SEASON_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid State Transition"));
    }

    @Test
    @DisplayName("GET seasons returns 401 for anonymous")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/leagues/{leagueId}/seasons", LEAGUE_ID))
                .andExpect(status().isUnauthorized());
    }
}
