package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.PlayerRequest;
import com.league.sportsleague.application.dto.PlayerResponse;
import com.league.sportsleague.application.dto.TeamResponse;
import com.league.sportsleague.application.service.TeamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TeamViewController.class)
@Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class TeamViewControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean TeamService teamService;
    @MockitoBean DataSource dataSource;

    private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private TeamResponse sample() {
        return new TeamResponse(ID, "Dinamo Zagreb", "DZG", "FOOTBALL", "Maksimir", "Zagreb", 1945);
    }

    private static final TeamResponse BASKET = new TeamResponse(
            UUID.fromString("33333333-3333-3333-3333-333333333333"),
            "Cibona", "CIB", "BASKETBALL", "Hall", "Zagreb", 1946);

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /teams?sport=BASKETBALL lists only teams of that sport")
    void listFiltersBySport() throws Exception {
        when(teamService.findAll()).thenReturn(List.of(sample(), BASKET));
        mockMvc.perform(get("/teams").param("sport", "BASKETBALL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedSport", "BASKETBALL"))
                .andExpect(model().attribute("teams", List.of(BASKET)));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /teams returns 200 with teams model")
    void listTeams() throws Exception {
        when(teamService.findAll()).thenReturn(List.of(sample()));
        mockMvc.perform(get("/teams"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/list"))
                .andExpect(model().attributeExists("teams"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /teams/{id} returns 200 with team model")
    void teamDetail() throws Exception {
        when(teamService.findById(ID)).thenReturn(sample());
        mockMvc.perform(get("/teams/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/detail"))
                .andExpect(model().attributeExists("team"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teams/new returns 200 for ADMIN")
    void newFormAdmin() throws Exception {
        mockMvc.perform(get("/teams/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/form"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /teams/new returns 403 for VIEWER")
    void newFormViewer() throws Exception {
        mockMvc.perform(get("/teams/new"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teams with blank name returns form with errors")
    void createValidationFails() throws Exception {
        mockMvc.perform(post("/teams").with(csrf())
                        .param("name", "")
                        .param("shortCode", "DZG")
                        .param("stadiumName", "Maksimir")
                        .param("city", "Zagreb"))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/form"));
    }

    // ── Squad ─────────────────────────────────────────────────────────────────

    private static final UUID PLAYER_ID = UUID.fromString("88888888-8888-8888-8888-888888888888");

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /teams/{id} renders the squad of the team")
    void teamDetailRendersSquad() throws Exception {
        when(teamService.findById(ID)).thenReturn(sample());
        when(teamService.findPlayers(ID)).thenReturn(
                List.of(new PlayerResponse(PLAYER_ID, ID, "Bruno Petkovic", 9, "Forward")));
        mockMvc.perform(get("/teams/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bruno Petkovic")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teams/{id}/players adds the player and redirects to the team page")
    void addPlayerRedirects() throws Exception {
        mockMvc.perform(post("/teams/{id}/players", ID).with(csrf())
                        .param("name", "Bruno Petkovic")
                        .param("shirtNumber", "9")
                        .param("position", "Forward"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/teams/" + ID))
                .andExpect(flash().attribute("flashKey", "teams.flash.playerAdded"));
        verify(teamService).addPlayer(ID, new PlayerRequest("Bruno Petkovic", 9, "Forward"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teams/{id}/players with a taken shirt number flashes the domain error")
    void addPlayerDuplicateNumberFlashesError() throws Exception {
        when(teamService.addPlayer(any(), any()))
                .thenThrow(new IllegalArgumentException("Shirt number 9 is already taken by Bruno Petkovic"));
        mockMvc.perform(post("/teams/{id}/players", ID).with(csrf())
                        .param("name", "Other")
                        .param("shirtNumber", "9"))
                .andExpect(redirectedUrl("/teams/" + ID))
                .andExpect(flash().attribute("errorMsg", "Shirt number 9 is already taken by Bruno Petkovic"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /teams/{id}/players without a shirt number flashes a validation error and adds nothing")
    void addPlayerValidationFails() throws Exception {
        mockMvc.perform(post("/teams/{id}/players", ID).with(csrf())
                        .param("name", "Bruno Petkovic"))
                .andExpect(redirectedUrl("/teams/" + ID))
                .andExpect(flash().attributeExists("errorMsg"));
        verify(teamService, never()).addPlayer(any(), any());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /teams/{id}/players returns 403 for OFFICIAL")
    void addPlayerForbiddenForOfficial() throws Exception {
        mockMvc.perform(post("/teams/{id}/players", ID).with(csrf())
                        .param("name", "Bruno Petkovic")
                        .param("shirtNumber", "9"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("GET /teams/{id}/players/{playerId}/edit renders the player form")
    void editPlayerForm() throws Exception {
        when(teamService.findById(ID)).thenReturn(sample());
        when(teamService.findPlayer(ID, PLAYER_ID)).thenReturn(
                new PlayerResponse(PLAYER_ID, ID, "Bruno Petkovic", 9, "Forward"));
        mockMvc.perform(get("/teams/{id}/players/{pid}/edit", ID, PLAYER_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("teams/player-form"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Bruno Petkovic")));
    }

    @Test
    @DisplayName("GET /teams returns 401 for anonymous (form-login redirect tested in SecurityAccessTest)")
    void anonymousRedirects() throws Exception {
        mockMvc.perform(get("/teams"))
                .andExpect(status().isUnauthorized());
    }
}
