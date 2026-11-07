package com.league.sportsleague.web.api;

import com.league.sportsleague.application.service.ChronicleService;
import com.league.sportsleague.domain.ai.ChronicleResult;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChronicleRestController.class)
@Import(com.league.sportsleague.infrastructure.security.MethodSecurityConfig.class)
class ChronicleRestControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ChronicleService chronicleService;

    private static final UUID FIXTURE_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /chronicle returns 200 with chronicle text")
    void returnsChronicle() throws Exception {
        when(chronicleService.generateChronicle(eq(FIXTURE_ID), any()))
                .thenReturn(new ChronicleResult("Dinamo won 2-1.", Language.ENGLISH));

        mockMvc.perform(post("/api/fixtures/{id}/chronicle", FIXTURE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Dinamo won 2-1."))
                .andExpect(jsonPath("$.language").value("ENGLISH"));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /chronicle?lang=CROATIAN passes CROATIAN to service")
    void passesLanguageParameter() throws Exception {
        when(chronicleService.generateChronicle(eq(FIXTURE_ID), eq(Language.CROATIAN)))
                .thenReturn(new ChronicleResult("Dinamo pobijedio 2-1.", Language.CROATIAN));

        mockMvc.perform(post("/api/fixtures/{id}/chronicle?lang=CROATIAN", FIXTURE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.language").value("CROATIAN"));
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("POST /chronicle returns 404 ProblemDetail when fixture not found")
    void returns404WhenFixtureNotFound() throws Exception {
        when(chronicleService.generateChronicle(any(), any()))
                .thenThrow(new EntityNotFoundException("Fixture", FIXTURE_ID));

        mockMvc.perform(post("/api/fixtures/{id}/chronicle", FIXTURE_ID).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("VIEWER can read the AI chronicle")
    void viewerCanReadChronicle() throws Exception {
        when(chronicleService.generateChronicle(eq(FIXTURE_ID), any()))
                .thenReturn(new ChronicleResult("Dinamo won 2-1.", Language.ENGLISH));
        mockMvc.perform(post("/api/fixtures/{id}/chronicle", FIXTURE_ID).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Dinamo won 2-1."));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /chronicle returns the stored or translated chronicle")
    void getReturnsStoredChronicle() throws Exception {
        when(chronicleService.findChronicle(eq(FIXTURE_ID), eq(Language.CROATIAN)))
                .thenReturn(Optional.of(new ChronicleResult("Izvještaj.", Language.CROATIAN)));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/fixtures/{id}/chronicle?lang=CROATIAN", FIXTURE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("Izvještaj."));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("GET /chronicle returns 204 when nothing was generated yet")
    void getReturnsNoContentWhenNothingStored() throws Exception {
        when(chronicleService.findChronicle(eq(FIXTURE_ID), any())).thenReturn(Optional.empty());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/fixtures/{id}/chronicle", FIXTURE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("an unavailable AI provider is reported as 503 ProblemDetail")
    void aiUnavailableIs503() throws Exception {
        when(chronicleService.findChronicle(any(), any()))
                .thenThrow(new com.league.sportsleague.domain.exception.ChronicleUnavailableException("busy"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/fixtures/{id}/chronicle", FIXTURE_ID))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.title").value("AI Provider Unavailable"));
    }

    @Test
    @DisplayName("anonymous is unauthorized")
    void anonymousIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/fixtures/{id}/chronicle", FIXTURE_ID).with(csrf()))
                .andExpect(status().isUnauthorized());
    }
}
