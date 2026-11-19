package com.league.sportsleague.infrastructure.security;

import com.league.sportsleague.application.dto.CreateLeagueRequest;
import com.league.sportsleague.application.dto.CreateTeamRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.service.ChronicleService;
import com.league.sportsleague.application.service.FixtureService;
import com.league.sportsleague.application.service.LeagueService;
import com.league.sportsleague.application.service.SeasonService;
import com.league.sportsleague.application.service.TeamService;
import com.league.sportsleague.domain.ai.Language;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.function.Executable;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The role rules are enforced on the services, not only on the controllers: a second caller of a service
 * (another controller, a future job, a console) cannot skip them. Real beans, real method security; the ids do
 * not exist, so a call that passes authorization ends in {@link EntityNotFoundException}.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:service_auth_testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class ServiceAuthorizationTest {

    private static final UUID NOBODY = UUID.fromString("00000000-0000-0000-0000-00000000dead");

    @Autowired LeagueService leagueService;
    @Autowired TeamService teamService;
    @Autowired SeasonService seasonService;
    @Autowired FixtureService fixtureService;
    @Autowired ChronicleService chronicleService;

    private Executable adminOnlyCall() {
        return () -> leagueService.createLeague(new CreateLeagueRequest("Authorization League", "authorization-league", "FOOTBALL", "Croatia"));
    }

    private Executable adminOnlyTeamCall() {
        return () -> teamService.deleteTeam(NOBODY);
    }

    private Executable adminOnlySeasonCall() {
        return () -> seasonService.activateSeason(NOBODY);
    }

    private Executable staffCall() {
        return () -> fixtureService.recordResult(NOBODY, new RecordResultRequest(1, 0));
    }

    private Executable staffAiCall() {
        return () -> chronicleService.generateChronicle(NOBODY, Language.ENGLISH);
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    @DisplayName("a VIEWER is refused by every write method of every service")
    void viewerIsRefusedEverywhere() {
        assertThrows(AccessDeniedException.class, adminOnlyCall());
        assertThrows(AccessDeniedException.class, adminOnlyTeamCall());
        assertThrows(AccessDeniedException.class, adminOnlySeasonCall());
        assertThrows(AccessDeniedException.class, staffCall());
        assertThrows(AccessDeniedException.class, staffAiCall());
    }

    @Test
    @WithMockUser(roles = "OFFICIAL")
    @DisplayName("an OFFICIAL may run the match and AI methods but not the administration methods")
    void officialIsStaffOnly() {
        assertThrows(AccessDeniedException.class, adminOnlyCall());
        assertThrows(AccessDeniedException.class, adminOnlyTeamCall());
        assertThrows(AccessDeniedException.class, adminOnlySeasonCall());
        assertThrows(EntityNotFoundException.class, staffCall());
        assertThrows(EntityNotFoundException.class, staffAiCall());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("an ADMIN passes every authorization check")
    void adminPassesEverything() {
        assertThrows(EntityNotFoundException.class, adminOnlyTeamCall());
        assertThrows(EntityNotFoundException.class, adminOnlySeasonCall());
        assertThrows(EntityNotFoundException.class, staffCall());
        assertThrows(EntityNotFoundException.class, staffAiCall());
    }

    @Test
    @DisplayName("without authentication a write method fails before running")
    void anonymousIsRefused() {
        assertThrows(AuthenticationCredentialsNotFoundException.class, adminOnlyTeamCall());
        assertThrows(AuthenticationCredentialsNotFoundException.class, staffCall());
    }

    @Test
    @DisplayName("reads stay open: the scheduler and the read-only pages call them without a role")
    void readsNeedNoRole() {
        assertThrows(EntityNotFoundException.class, () -> seasonService.findById(NOBODY));
        assertDoesNotThrow(() -> teamService.findAll());
        assertDoesNotThrow(() -> leagueService.findAll());
    }
}
