package com.league.sportsleague.web.view;

import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.MatchEventResponse;
import com.league.sportsleague.application.dto.MatchSheetResponse;
import com.league.sportsleague.application.service.FixtureService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FixtureViewController.class)
@Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class FixtureViewControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean FixtureService fixtureService;
    @MockitoBean DataSource dataSource;

    private static final UUID FIXTURE_ID   = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ROUND_ID     = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID HOME_TEAM_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final UUID AWAY_TEAM_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");

    private FixtureResponse sampleFixture() {
        return new FixtureResponse(FIXTURE_ID, ROUND_ID, 1, "Round 1",
                "DIN", "Dinamo", HOME_TEAM_ID, "HAJ", "Hajduk", AWAY_TEAM_ID,
                null, null, "SCHEDULED", Instant.now(), List.of());
    }

    private MatchSheetResponse sampleSheet() {
        return new MatchSheetResponse(11, 12,
                new MatchSheetResponse.TeamSheet(HOME_TEAM_ID, "Dinamo", false, List.of(), List.of(), List.of(), List.of()),
                new MatchSheetResponse.TeamSheet(AWAY_TEAM_ID, "Hajduk", false, List.of(), List.of(), List.of(), List.of()));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /fixtures/{id} renders the detail view with fixture and sheet models")
    void detailViewRenders() throws Exception {
        when(fixtureService.findById(FIXTURE_ID)).thenReturn(sampleFixture());
        when(fixtureService.getMatchSheet(FIXTURE_ID)).thenReturn(sampleSheet());

        mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("fixtures/detail"))
                .andExpect(model().attributeExists("fixture"))
                .andExpect(model().attributeExists("sheet"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lineups")));
    }

    // ── Rendering of the staff-only parts (the VIEWER test above never reaches the Actions pane) ──

    private static final UUID PLAYER_9  = UUID.fromString("99999999-0000-0000-0000-000000000009");
    private static final UUID PLAYER_12 = UUID.fromString("99999999-0000-0000-0000-000000000012");
    private static final UUID PLAYER_4  = UUID.fromString("99999999-0000-0000-0000-000000000004");

    private FixtureResponse fixture(String status, List<MatchEventResponse> events) {
        return new FixtureResponse(FIXTURE_ID, ROUND_ID, 1, "Round 1",
                "DIN", "Dinamo", HOME_TEAM_ID, "HAJ", "Hajduk", AWAY_TEAM_ID,
                "SCHEDULED".equals(status) ? null : 0, "SCHEDULED".equals(status) ? null : 0,
                status, Instant.now(), events);
    }

    /** Home lineup filed (one starter as captain, one substitute, one suspended squad player); away: nothing. */
    private MatchSheetResponse sheetWithHomeLineup() {
        var starter = new MatchSheetResponse.LineupPlayer(PLAYER_9, 9, "Bruno Petkovic", "Forward", true);
        var substitute = new MatchSheetResponse.LineupPlayer(PLAYER_12, 12, "Sandro Kulenovic", null, false);
        var suspended = new MatchSheetResponse.SquadOption(PLAYER_4, 4, "Kevin Theophile", "Defender", null, false, "RED_CARD");
        var squad = List.of(
                suspended,
                new MatchSheetResponse.SquadOption(PLAYER_9, 9, "Bruno Petkovic", "Forward", "STARTER", true, null),
                new MatchSheetResponse.SquadOption(PLAYER_12, 12, "Sandro Kulenovic", null, "SUBSTITUTE", false, null));
        return new MatchSheetResponse(11, 12,
                new MatchSheetResponse.TeamSheet(HOME_TEAM_ID, "Dinamo", true,
                        List.of(starter), List.of(substitute), squad, List.of(suspended)),
                new MatchSheetResponse.TeamSheet(AWAY_TEAM_ID, "Hajduk", false, List.of(), List.of(), List.of(), List.of()));
    }

    private static org.hamcrest.Matcher<String> has(String text) {
        return org.hamcrest.Matchers.containsString(text);
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("GET /fixtures/{id} as OFFICIAL before kick-off renders the lineup editor and warns about the missing lineup")
    void scheduledFixtureRendersLineupEditor() throws Exception {
        when(fixtureService.findById(FIXTURE_ID)).thenReturn(fixture("SCHEDULED", List.of()));
        when(fixtureService.getMatchSheet(FIXTURE_ID)).thenReturn(sheetWithHomeLineup());

        mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(has("Match sheet: lineups")))
                .andExpect(content().string(has("Select exactly 11 starters and up to 12 substitutes.")))
                .andExpect(content().string(has("name=\"starterIds\"")))
                .andExpect(content().string(has("Save lineup")))
                .andExpect(content().string(has("Clear lineup")))
                .andExpect(content().string(has("red card")))
                .andExpect(content().string(has("Suspended for this match")))
                .andExpect(content().string(has("This team has no registered players.")))
                .andExpect(content().string(has("No lineup submitted for: Hajduk")))
                .andExpect(content().string(has("Kick off")));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("GET /fixtures/{id} names both teams in the warning when neither lineup is filed")
    void scheduledFixtureWarnsAboutBothMissingLineups() throws Exception {
        when(fixtureService.findById(FIXTURE_ID)).thenReturn(sampleFixture());
        when(fixtureService.getMatchSheet(FIXTURE_ID)).thenReturn(sampleSheet());

        mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(has("No lineup submitted for: Dinamo, Hajduk")));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("GET /fixtures/{id} in progress renders the event form with lineup players and the substitution text")
    void inProgressFixtureRendersEventForm() throws Exception {
        var substitution = new MatchEventResponse(UUID.randomUUID(), 60, "SUBSTITUTION",
                "Dinamo", "DIN", "Bruno Petkovic", "Sandro Kulenovic");
        when(fixtureService.findById(FIXTURE_ID)).thenReturn(fixture("IN_PROGRESS", List.of(substitution)));
        when(fixtureService.getMatchSheet(FIXTURE_ID)).thenReturn(sheetWithHomeLineup());

        mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(has("Final whistle")))
                .andExpect(content().string(has("name=\"playerId\"")))
                .andExpect(content().string(has("name=\"relatedPlayerId\"")))
                .andExpect(content().string(has("9 Bruno Petkovic")))
                .andExpect(content().string(has("12 Sandro Kulenovic")))
                .andExpect(content().string(has("(on: Sandro Kulenovic)")))
                .andExpect(content().string(org.hamcrest.Matchers.not(has("name=\"starterIds\""))));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /fixtures/{id}/lineups/{teamId} submits the lineup and redirects with flash")
    void submitLineupRedirects() throws Exception {
        UUID p1 = UUID.randomUUID();
        mockMvc.perform(post("/fixtures/{id}/lineups/{tid}", FIXTURE_ID, HOME_TEAM_ID).with(csrf())
                        .param("starterIds", p1.toString())
                        .param("captainId", p1.toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fixtures/" + FIXTURE_ID))
                .andExpect(flash().attribute("flashKey", "fixtures.flash.lineupSaved"));

        verify(fixtureService).submitLineup(eq(FIXTURE_ID), eq(HOME_TEAM_ID), any());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /fixtures/{id}/lineups/{teamId} with bad player count flashes domain error")
    void submitLineupDomainErrorFlashes() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Exactly 11 starters are required, got 1"))
                .when(fixtureService).submitLineup(eq(FIXTURE_ID), eq(HOME_TEAM_ID), any());

        mockMvc.perform(post("/fixtures/{id}/lineups/{tid}", FIXTURE_ID, HOME_TEAM_ID).with(csrf())
                        .param("starterIds", UUID.randomUUID().toString()))
                .andExpect(redirectedUrl("/fixtures/" + FIXTURE_ID))
                .andExpect(flash().attribute("errorMsg", "Exactly 11 starters are required, got 1"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("POST /fixtures/{id}/lineups/{teamId} is forbidden for VIEWER")
    void submitLineupForbiddenForViewer() throws Exception {
        mockMvc.perform(post("/fixtures/{id}/lineups/{tid}", FIXTURE_ID, HOME_TEAM_ID).with(csrf())
                        .param("starterIds", UUID.randomUUID().toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("POST /fixtures/{id}/lineups/{teamId}/clear clears the lineup and redirects")
    void clearLineupRedirects() throws Exception {
        mockMvc.perform(post("/fixtures/{id}/lineups/{tid}/clear", FIXTURE_ID, HOME_TEAM_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/fixtures/" + FIXTURE_ID))
                .andExpect(flash().attribute("flashKey", "fixtures.flash.lineupCleared"));

        verify(fixtureService).clearLineup(FIXTURE_ID, HOME_TEAM_ID);
    }
}
