package com.league.sportsleague.web.view;

import com.league.sportsleague.application.service.LeagueService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@org.springframework.context.annotation.Import(DisplayFormats.class)
class DashboardControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean LeagueService leagueService;
    @MockitoBean com.league.sportsleague.application.service.SeasonService seasonService;
    @MockitoBean com.league.sportsleague.application.service.TeamService teamService;
    @MockitoBean DataSource dataSource;

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET / returns 200 with dashboard view")
    void dashboardLoads() throws Exception {
        when(leagueService.findAll()).thenReturn(List.of());
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attributeExists("leagues"))
                .andExpect(model().attribute("seasonCount", 0))
                .andExpect(model().attribute("activeSeasonCount", 0L))
                .andExpect(model().attribute("teamCount", 0));
    }

    @Test
    @DisplayName("GET / returns 401 for anonymous (form-login redirect tested in SecurityAccessTest)")
    void anonymousRedirects() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isUnauthorized());
    }
}
