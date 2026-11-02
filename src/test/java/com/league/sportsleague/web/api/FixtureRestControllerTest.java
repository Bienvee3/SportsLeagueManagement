package com.league.sportsleague.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.service.FixtureService;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FixtureRestController.class)
@org.springframework.context.annotation.Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class FixtureRestControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean FixtureService fixtureService;

    private static final UUID FIXTURE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ROUND_ID   = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID SEASON_ID  = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private FixtureResponse sample() {
        return new FixtureResponse(FIXTURE_ID, "HFC", "Home FC",
                "AFC", "Away FC", null, null, "SCHEDULED", Instant.now());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/fixtures/{id} returns 200")
    void getById() throws Exception {
        when(fixtureService.findById(FIXTURE_ID)).thenReturn(sample());
        mockMvc.perform(get("/api/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/fixtures/{id} returns 404 ProblemDetail")
    void getByIdNotFound() throws Exception {
        when(fixtureService.findById(FIXTURE_ID))
                .thenThrow(new EntityNotFoundException("Fixture", FIXTURE_ID));
        mockMvc.perform(get("/api/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/fixtures/by-round/{roundId} returns 200")
    void byRound() throws Exception {
        when(fixtureService.findByRound(ROUND_ID)).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/fixtures/by-round/{roundId}", ROUND_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].homeTeamName").value("Home FC"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/fixtures/by-season/{seasonId} returns 200")
    void bySeason() throws Exception {
        when(fixtureService.findBySeasonWithTeams(SEASON_ID)).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/fixtures/by-season/{seasonId}", SEASON_ID))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /api/fixtures/{id}/result returns 204")
    void recordResult() throws Exception {
        var request = new RecordResultRequest(2, 1);
        mockMvc.perform(post("/api/fixtures/{id}/result", FIXTURE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/fixtures/{id}/result returns 403 for VIEWER")
    void recordResultForbiddenForViewer() throws Exception {
        var request = new RecordResultRequest(2, 1);
        mockMvc.perform(post("/api/fixtures/{id}/result", FIXTURE_ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /api/fixtures/{id}/cancel returns 204")
    void cancel() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/cancel", FIXTURE_ID).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/fixtures/{id} returns 401 for anonymous")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isUnauthorized());
    }
}
