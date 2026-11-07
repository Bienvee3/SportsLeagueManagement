package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CreateSeasonRequest;
import com.league.sportsleague.application.dto.SeasonResponse;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock LeagueRepository leagueRepository;
    @Mock TeamRepository teamRepository;
    @Mock SeasonRepository seasonRepository;
    @Mock RoundRepository roundRepository;
    @Mock FixtureRepository fixtureRepository;

    @InjectMocks SeasonService seasonService;

    private League league;
    private Season season;
    private Team team;

    @BeforeEach
    void setUp() {
        league = new League("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
        season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
        team = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
    }

    // ── createSeason ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("createSeason saves a Season under the given league and returns its UUID")
    void createSeasonSavesAndReturnsId() {
        UUID leagueId = league.getId();
        when(leagueRepository.findById(leagueId)).thenReturn(Optional.of(league));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID id = seasonService.createSeason(leagueId, new CreateSeasonRequest("2026/2027", 2026, 2027));

        assertNotNull(id);
        ArgumentCaptor<Season> captor = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(captor.capture());
        assertEquals("2026/2027", captor.getValue().getName());
        assertEquals(league, captor.getValue().getLeague());
    }

    @Test
    @DisplayName("createSeason throws EntityNotFoundException when league not found")
    void createSeasonThrowsWhenLeagueNotFound() {
        when(leagueRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.createSeason(UUID.randomUUID(), new CreateSeasonRequest("S", 2026, 2027)));
    }

    // ── activateSeason ────────────────────────────────────────────────────────

    @Test
    @DisplayName("activateSeason calls season.activate() and saves")
    void activateSeasonCallsDomainMethodAndSaves() {
        UUID id = season.getId();
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.activateSeason(id);

        assertEquals(SeasonStatus.ACTIVE, season.getStatus());
        verify(seasonRepository).save(season);
    }

    @Test
    @DisplayName("activateSeason throws EntityNotFoundException when season not found")
    void activateSeasonThrowsWhenNotFound() {
        when(seasonRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> seasonService.activateSeason(UUID.randomUUID()));
    }

    @Test
    @DisplayName("activateSeason propagates domain guard (ACTIVE -> activate throws)")
    void activateSeasonPropagatesDomainGuard() {
        season.activate(); // now ACTIVE
        UUID id = season.getId();
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));

        assertThrows(InvalidStateTransitionException.class, () -> seasonService.activateSeason(id));
    }

    // ── completeSeason ────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeSeason calls season.complete() after activating")
    void completeSeasonCallsDomainMethod() {
        season.activate();
        UUID id = season.getId();
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.completeSeason(id);

        assertEquals(SeasonStatus.COMPLETED, season.getStatus());
        verify(seasonRepository).save(season);
    }

    // ── registerTeam ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("registerTeam adds SeasonTeam to season and saves")
    void registerTeamAddsToseasonAndSaves() {
        UUID seasonId = season.getId();
        UUID teamId = team.getId();
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.registerTeam(seasonId, teamId, 1);

        assertEquals(1, season.getSeasonTeams().size());
        assertEquals(team, season.getSeasonTeams().get(0).getTeam());
        verify(seasonRepository).save(season);
    }

    @Test
    @DisplayName("registerTeam throws EntityNotFoundException when season not found")
    void registerTeamThrowsWhenSeasonNotFound() {
        when(seasonRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.registerTeam(UUID.randomUUID(), UUID.randomUUID(), 0));
    }

    @Test
    @DisplayName("registerTeam throws EntityNotFoundException when team not found")
    void registerTeamThrowsWhenTeamNotFound() {
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(teamRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.registerTeam(season.getId(), UUID.randomUUID(), 0));
    }

    @Test
    @DisplayName("registerTeam rejects a team whose sport differs from the league sport")
    void registerTeamRejectsSportMismatch() {
        Team basketTeam = new Team("Cibona", "CIB", "BASKETBALL", "Hall", "Zagreb", 1946);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(teamRepository.findById(basketTeam.getId())).thenReturn(Optional.of(basketTeam));

        assertThrows(IllegalArgumentException.class,
                () -> seasonService.registerTeam(season.getId(), basketTeam.getId(), 1));
        verify(seasonRepository, never()).save(any());
    }

    // ── createRound / addFixture ──────────────────────────────────────────────

    private Team confirmedTeam(String code) {
        Team t = new Team("Team " + code, code, "Stadium", "City", 2000);
        season.addSeasonTeam(new SeasonTeam(season, t, 1, SeasonTeamStatus.CONFIRMED));
        return t;
    }

    @Test
    @DisplayName("createRound numbers the new round after the last existing one")
    void createRoundUsesNextNumber() {
        season.addRound(new Round(season, 1, "Round 1", null, null, null));
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.createRound(season.getId(), null, null);

        assertEquals(2, season.getRounds().size());
        Round created = season.getRounds().get(1);
        assertEquals(2, created.getRoundNumber());
        assertEquals("Round 2", created.getName());
    }

    @Test
    @DisplayName("createRound is rejected on a COMPLETED season")
    void createRoundRejectedWhenCompleted() {
        Season done = new Season(league, "2025/2026", 2025, 2026, SeasonStatus.COMPLETED);
        when(seasonRepository.findById(done.getId())).thenReturn(Optional.of(done));
        assertThrows(IllegalStateException.class,
                () -> seasonService.createRound(done.getId(), "Extra", null));
    }

    @Test
    @DisplayName("addFixture adds a match between two confirmed teams to the round")
    void addFixtureAddsMatch() {
        Team a = confirmedTeam("AAA"); Team b = confirmedTeam("BBB");
        Round round = new Round(season, 1, "Round 1", null, null, null);
        season.addRound(round);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.addFixture(season.getId(), round.getId(), a.getId(), b.getId(),
                java.time.Instant.parse("2026-12-01T18:00:00Z"));

        assertEquals(1, round.getFixtures().size());
        assertEquals(a, round.getFixtures().get(0).getHomeTeam());
    }

    @Test
    @DisplayName("addFixture rejects a team that already plays in that round")
    void addFixtureRejectsTeamAlreadyInRound() {
        Team a = confirmedTeam("AAA"); Team b = confirmedTeam("BBB"); Team c = confirmedTeam("CCC");
        Round round = new Round(season, 1, "Round 1", null, null, null);
        season.addRound(round);
        round.addFixture(new Fixture(round, a, b, java.time.Instant.now()));
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));

        assertThrows(IllegalArgumentException.class, () -> seasonService.addFixture(
                season.getId(), round.getId(), a.getId(), c.getId(), java.time.Instant.now()));
    }

    @Test
    @DisplayName("addFixture rejects a team not registered in the season")
    void addFixtureRejectsUnregisteredTeam() {
        Team a = confirmedTeam("AAA");
        Team outsider = new Team("Out", "OUT", "S", "C", 2000);
        Round round = new Round(season, 1, "Round 1", null, null, null);
        season.addRound(round);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));

        assertThrows(IllegalArgumentException.class, () -> seasonService.addFixture(
                season.getId(), round.getId(), a.getId(), outsider.getId(), java.time.Instant.now()));
    }

    // ── deleteFixture / deleteRound ───────────────────────────────────────────

    private Round roundWithMatch(Team a, Team b, boolean finished) {
        Round round = new Round(season, 1, "Round 1", null, null, null);
        season.addRound(round);
        Fixture f = new Fixture(round, a, b, java.time.Instant.now());
        round.addFixture(f);
        if (finished) f.recordResult(new com.league.sportsleague.domain.valueobject.Score(1, 0));
        return round;
    }

    @Test
    @DisplayName("deleteFixture removes a scheduled match from its round")
    void deleteFixtureRemovesScheduledMatch() {
        Round round = roundWithMatch(confirmedTeam("AAA"), confirmedTeam("BBB"), false);
        Fixture f = round.getFixtures().get(0);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.deleteFixture(season.getId(), round.getId(), f.getId());

        assertTrue(round.getFixtures().isEmpty());
    }

    @Test
    @DisplayName("deleteFixture refuses a FINISHED match")
    void deleteFixtureRefusesFinished() {
        Round round = roundWithMatch(confirmedTeam("AAA"), confirmedTeam("BBB"), true);
        Fixture f = round.getFixtures().get(0);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));

        assertThrows(IllegalStateException.class,
                () -> seasonService.deleteFixture(season.getId(), round.getId(), f.getId()));
        assertEquals(1, round.getFixtures().size());
    }

    @Test
    @DisplayName("deleteRound removes the round and its scheduled matches")
    void deleteRoundRemovesRound() {
        Round round = roundWithMatch(confirmedTeam("AAA"), confirmedTeam("BBB"), false);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.deleteRound(season.getId(), round.getId());

        assertTrue(season.getRounds().isEmpty());
    }

    @Test
    @DisplayName("deleteRound refuses a round that contains a FINISHED match")
    void deleteRoundRefusesFinishedMatch() {
        Round round = roundWithMatch(confirmedTeam("AAA"), confirmedTeam("BBB"), true);
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));

        assertThrows(IllegalStateException.class,
                () -> seasonService.deleteRound(season.getId(), round.getId()));
        assertEquals(1, season.getRounds().size());
    }

    // ── withdrawTeam ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("withdrawTeam calls seasonTeam.withdraw() and saves")
    void withdrawTeamCallsWithdrawAndSaves() {
        SeasonTeam st = new SeasonTeam(season, team, 1, SeasonTeamStatus.CONFIRMED);
        season.addSeasonTeam(st);
        UUID seasonId = season.getId();
        UUID teamId = team.getId();
        when(seasonRepository.findById(seasonId)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.withdrawTeam(seasonId, teamId);

        assertEquals(SeasonTeamStatus.WITHDRAWN, st.getStatus());
        verify(seasonRepository).save(season);
    }

    @Test
    @DisplayName("withdrawTeam throws EntityNotFoundException when team not enrolled")
    void withdrawTeamThrowsWhenTeamNotEnrolled() {
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.withdrawTeam(season.getId(), UUID.randomUUID()));
    }

    // ── completeRound ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("completeRound calls round.completeRound() and saves")
    void completeRoundCallsDomainMethodAndSaves() {
        Round round = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(3),
                RoundStatus.SCHEDULED);
        round.startRound();
        UUID roundId = round.getId();
        when(roundRepository.findById(roundId)).thenReturn(Optional.of(round));
        when(roundRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.completeRound(roundId);

        assertEquals(RoundStatus.COMPLETED, round.getStatus());
        verify(roundRepository).save(round);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns mapped SeasonResponse")
    void findByIdReturnsResponse() {
        UUID id = season.getId();
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));

        SeasonResponse response = seasonService.findById(id);
        assertEquals("2026/2027", response.name());
        assertEquals("UPCOMING", response.status());
    }

    // ── findByLeague ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByLeague returns list of SeasonResponse for that league")
    void findByLeagueReturnsResponses() {
        when(seasonRepository.findByLeagueId(league.getId())).thenReturn(List.of(season));

        List<SeasonResponse> results = seasonService.findByLeague(league.getId());
        assertEquals(1, results.size());
        assertEquals("2026/2027", results.get(0).name());
    }

    // ── generateSchedule ──────────────────────────────────────────────────────

    @Test
    @DisplayName("generateSchedule builds rounds for CONFIRMED teams and saves the season")
    void generateScheduleSavesRoundsForConfirmedTeams() {
        UUID id = season.getId();
        for (String code : List.of("AAA", "BBB", "CCC", "DDD")) {
            season.addSeasonTeam(new SeasonTeam(season,
                    new Team("Team " + code, code, "Stadium", "City", 1900), 1, SeasonTeamStatus.CONFIRMED));
        }
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.generateSchedule(id, Instant.parse("2026-09-01T18:00:00Z"));

        ArgumentCaptor<Season> captor = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(captor.capture());
        List<Round> rounds = captor.getValue().getRounds();
        assertEquals(3, rounds.size());
        assertEquals(6, rounds.stream().mapToInt(r -> r.getFixtures().size()).sum());
    }

    @Test
    @DisplayName("generateSchedule excludes WITHDRAWN teams from the schedule")
    void generateScheduleExcludesWithdrawnTeams() {
        UUID id = season.getId();
        for (String code : List.of("AAA", "BBB", "CCC")) {
            season.addSeasonTeam(new SeasonTeam(season,
                    new Team("Team " + code, code, "Stadium", "City", 1900), 1, SeasonTeamStatus.CONFIRMED));
        }
        Team withdrawnTeam = new Team("Team DDD", "DDD", "Stadium", "City", 1900);
        SeasonTeam withdrawn = new SeasonTeam(season, withdrawnTeam, 4, SeasonTeamStatus.CONFIRMED);
        season.addSeasonTeam(withdrawn);
        withdrawn.withdraw();
        when(seasonRepository.findById(id)).thenReturn(Optional.of(season));
        when(seasonRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        seasonService.generateSchedule(id, Instant.parse("2026-09-01T18:00:00Z"));

        ArgumentCaptor<Season> captor = ArgumentCaptor.forClass(Season.class);
        verify(seasonRepository).save(captor.capture());
        List<Round> rounds = captor.getValue().getRounds();
        assertEquals(3, rounds.size());
        assertEquals(3, rounds.stream().mapToInt(r -> r.getFixtures().size()).sum());
        rounds.forEach(r -> r.getFixtures().forEach(f -> {
            assertNotEquals(withdrawnTeam, f.getHomeTeam());
            assertNotEquals(withdrawnTeam, f.getAwayTeam());
        }));
    }

    @Test
    @DisplayName("generateSchedule throws EntityNotFoundException when season not found")
    void generateScheduleThrowsWhenSeasonNotFound() {
        when(seasonRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.generateSchedule(UUID.randomUUID(), Instant.now()));
    }

    // ── getStandings ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("getStandings throws EntityNotFoundException when season not found")
    void getStandingsThrowsWhenSeasonNotFound() {
        when(seasonRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> seasonService.getStandings(UUID.randomUUID()));
    }

    @Test
    @DisplayName("getStandings returns empty list for season with no finished fixtures")
    void getStandingsReturnsEmptyForNoFixtures() {
        when(seasonRepository.findById(season.getId())).thenReturn(Optional.of(season));
        when(fixtureRepository.findFinishedBySeasonWithTeams(season.getId())).thenReturn(List.of());
        var response = seasonService.getStandings(season.getId());
        assertNotNull(response);
        assertTrue(response.entries().isEmpty());
    }
}
