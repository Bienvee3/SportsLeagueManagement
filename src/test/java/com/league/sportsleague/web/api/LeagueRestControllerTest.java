package com.league.sportsleague.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.LeagueResponse;
import com.league.sportsleague.application.dto.UpdateLeagueRequest;
import com.league.sportsleague.application.service.LeagueService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeagueRestController.class)
@org.springframework.context.annotation.Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class LeagueRestControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean LeagueService leagueService;

    private static final UUID ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private LeagueResponse sampleResponse() {
        return new LeagueResponse(ID, "SuperSport HNL", "supersport-hnl",
                "FOOTBALL", "Croatia", Instant.now());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues returns 200 with list")
    void listAll() throws Exception {
        when(leagueService.findAll()).thenReturn(List.of(sampleResponse()));
        mockMvc.perform(get("/api/leagues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("SuperSport HNL"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues/{id} returns 200")
    void getById() throws Exception {
        when(leagueService.findById(ID)).thenReturn(sampleResponse());
        mockMvc.perform(get("/api/leagues/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("supersport-hnl"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues/{id} returns 404 ProblemDetail when not found")
    void getByIdNotFound() throws Exception {
        when(leagueService.findById(ID)).thenThrow(new EntityNotFoundException("League", ID));
        mockMvc.perform(get("/api/leagues/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/leagues/slug/{slug} returns 200")
    void getBySlug() throws Exception {
        when(leagueService.findBySlug("supersport-hnl")).thenReturn(sampleResponse());
        mockMvc.perform(get("/api/leagues/slug/supersport-hnl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.country").value("Croatia"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/leagues creates league and returns 201 with Location")
    void create() throws Exception {
        when(leagueService.createLeague(any())).thenReturn(ID);
        var request = new CreateLeagueRequest("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
        mockMvc.perform(post("/api/leagues").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(ID.toString())));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/leagues returns 403 for VIEWER")
    void createForbiddenForViewer() throws Exception {
        var request = new CreateLeagueRequest("League", "league", "FOOTBALL", "Croatia");
        mockMvc.perform(post("/api/leagues").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/leagues returns 400 when name is blank")
    void createValidationFails() throws Exception {
        var request = new CreateLeagueRequest("", "supersport-hnl", "FOOTBALL", "Croatia");
        mockMvc.perform(post("/api/leagues").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/leagues/{id} returns 204")
    void update() throws Exception {
        var request = new UpdateLeagueRequest("New Name", "Croatia", "FOOTBALL");
        mockMvc.perform(put("/api/leagues/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        verify(leagueService).rename(eq(ID), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("DELETE /api/leagues/{id} returns 204")
    void deleteLeague() throws Exception {
        mockMvc.perform(delete("/api/leagues/{id}", ID).with(csrf()))
                .andExpect(status().isNoContent());
        verify(leagueService).delete(ID);
    }

    @Test
    @DisplayName("GET /api/leagues returns 401 for anonymous")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/leagues"))
                .andExpect(status().isUnauthorized());
    }
}
