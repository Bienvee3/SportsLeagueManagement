package com.league.sportsleague.web.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.PlayerResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.service.TeamService;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamRestController.class)
@org.springframework.context.annotation.Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class TeamRestControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    @MockitoBean TeamService teamService;

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private TeamResponse sample() {
        return new TeamResponse(ID, "Dinamo Zagreb", "DZG", "FOOTBALL", "Maksimir", "Zagreb", 1945);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/teams returns 200 with list")
    void listAll() throws Exception {
        when(teamService.findAll()).thenReturn(List.of(sample()));
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Dinamo Zagreb"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/teams/{id} returns 200")
    void getById() throws Exception {
        when(teamService.findById(ID)).thenReturn(sample());
        mockMvc.perform(get("/api/teams/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("DZG"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/teams/{id} returns 404 ProblemDetail")
    void getByIdNotFound() throws Exception {
        when(teamService.findById(ID)).thenThrow(new EntityNotFoundException("Team", ID));
        mockMvc.perform(get("/api/teams/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/teams returns 201 with Location")
    void create() throws Exception {
        when(teamService.createTeam(any())).thenReturn(ID);
        var request = new CreateTeamRequest("Dinamo Zagreb", "DZG", "FOOTBALL", "Maksimir", "Zagreb", 1945);
        mockMvc.perform(post("/api/teams").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString(ID.toString())));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /api/teams returns 403 for VIEWER")
    void createForbiddenForViewer() throws Exception {
        var request = new CreateTeamRequest("Team", "TM", "FOOTBALL", "Stadium", "City", null);
        mockMvc.perform(post("/api/teams").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/teams returns 400 when name is blank")
    void createValidationFails() throws Exception {
        var request = new CreateTeamRequest("", "DZG", "FOOTBALL", "Maksimir", "Zagreb", 1945);
        mockMvc.perform(post("/api/teams").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("PUT /api/teams/{id} returns 204")
    void update() throws Exception {
        var request = new CreateTeamRequest("Dinamo Zagreb", "DZG", "FOOTBALL", "Maksimir", "Zagreb", 1945);
        mockMvc.perform(put("/api/teams/{id}", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
        verify(teamService).updateProfile(ID, "Dinamo Zagreb", "Maksimir", "Zagreb", 1945);
    }

    // ── Squad ─────────────────────────────────────────────────────────────────

    private static final UUID PLAYER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /api/teams/{id}/players returns the squad")
    void listPlayers() throws Exception {
        when(teamService.findPlayers(ID)).thenReturn(
                List.of(new PlayerResponse(PLAYER_ID, ID, "Bruno Petkovic", 9, "Forward")));
        mockMvc.perform(get("/api/teams/{id}/players", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Bruno Petkovic"))
                .andExpect(jsonPath("$[0].shirtNumber").value(9));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/teams/{id}/players returns 201 with the player Location")
    void addPlayer() throws Exception {
        when(teamService.addPlayer(any(), any())).thenReturn(PLAYER_ID);
        mockMvc.perform(post("/api/teams/{id}/players", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayerRequest("Bruno Petkovic", 9, "Forward"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        org.hamcrest.Matchers.endsWith("/api/teams/" + ID + "/players/" + PLAYER_ID)));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /api/teams/{id}/players returns 403 for OFFICIAL")
    void addPlayerForbiddenForOfficial() throws Exception {
        mockMvc.perform(post("/api/teams/{id}/players", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayerRequest("Bruno Petkovic", 9, "Forward"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/teams/{id}/players returns 400 when the name is blank")
    void addPlayerValidationFails() throws Exception {
        mockMvc.perform(post("/api/teams/{id}/players", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayerRequest("", 9, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /api/teams/{id}/players returns 400 when the shirt number is taken")
    void addPlayerDuplicateNumber() throws Exception {
        when(teamService.addPlayer(any(), any()))
                .thenThrow(new IllegalArgumentException("Shirt number 9 is already taken by Bruno Petkovic"));
        mockMvc.perform(post("/api/teams/{id}/players", ID).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PlayerRequest("Other", 9, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Shirt number 9 is already taken by Bruno Petkovic"));
    }

    @Test
    @DisplayName("GET /api/teams returns 401 for anonymous")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }
}
