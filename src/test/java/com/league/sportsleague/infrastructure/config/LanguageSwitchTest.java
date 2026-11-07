package com.league.sportsleague.infrastructure.config;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:lang_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class LanguageSwitchTest {

    private static final String FIXTURE_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("?appLang=hr stores Croatian in the app-lang cookie")
    void appLangParamSetsCookie() throws Exception {
        mockMvc.perform(get("/leagues").param("appLang", "hr"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("app-lang", "hr"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("the chronicle API's own ?lang=CROATIAN never changes the app language")
    void chronicleLangParamDoesNotTouchAppLanguage() throws Exception {
        mockMvc.perform(get("/leagues").param("lang", "CROATIAN"))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("app-lang"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("the language cookie drives the chronicle language on the fixture page")
    void cookieDrivesChronicleLanguage() throws Exception {
        String hr = mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID).cookie(new Cookie("app-lang", "hr")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String en = mockMvc.perform(get("/fixtures/{id}", FIXTURE_ID))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertTrue(hr.contains("data-chronicle-lang=\"CROATIAN\""));
        assertTrue(en.contains("data-chronicle-lang=\"ENGLISH\""));
    }
}
