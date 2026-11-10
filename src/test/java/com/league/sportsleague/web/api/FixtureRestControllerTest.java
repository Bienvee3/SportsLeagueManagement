package com.league.sportsleague.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.service.FixtureService;
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
    private static final UUID HOME_TEAM_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID AWAY_TEAM_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
    private FixtureResponse sample() {
        return new FixtureResponse(FIXTURE_ID, ROUND_ID, 1, "Round 1",
                "HFC", "Home FC", HOME_TEAM_ID, "AFC", "Away FC", AWAY_TEAM_ID,
                null, null, "SCHEDULED", Instant.now(), java.util.List.of());
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
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("DELETE /api/fixtures/{id}/events/{eventId} returns 204")
    void deleteEvent() throws Exception {
        UUID eventId = UUID.fromString("99999999-9999-9999-9999-999999999999");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/fixtures/{id}/events/{e}", FIXTURE_ID, eventId).with(csrf()))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(fixtureService).deleteMatchEvent(FIXTURE_ID, eventId);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("DELETE event is forbidden for VIEWER")
    void deleteEventForbiddenForViewer() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/fixtures/{id}/events/{e}", FIXTURE_ID, UUID.randomUUID()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /api/fixtures/{id}/kick-off returns 204 for OFFICIAL")
    void kickOff() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/kick-off", FIXTURE_ID).with(csrf()))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(fixtureService).kickOff(FIXTURE_ID);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/fixtures/{id}/kick-off returns 403 for VIEWER")
    void kickOffForbiddenForViewer() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/kick-off", FIXTURE_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /api/fixtures/{id}/kick-off returns 409 when the match has already started")
    void kickOffConflict() throws Exception {
        org.mockito.Mockito.doThrow(new InvalidStateTransitionException("Fixture", "IN_PROGRESS", "kickOff"))
                .when(fixtureService).kickOff(FIXTURE_ID);
        mockMvc.perform(post("/api/fixtures/{id}/kick-off", FIXTURE_ID).with(csrf()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Invalid State Transition"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/fixtures/{id}/finish returns 204 for ADMIN")
    void finish() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/finish", FIXTURE_ID).with(csrf()))
                .andExpect(status().isNoContent());
        org.mockito.Mockito.verify(fixtureService).finish(FIXTURE_ID);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/fixtures/{id}/finish returns 403 for VIEWER")
    void finishForbiddenForViewer() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/finish", FIXTURE_ID).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/fixtures/{id} returns 401 for anonymous")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isUnauthorized());
    }
}
