package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.repository.*;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class FixtureServiceTest {

    @Mock FixtureRepository fixtureRepository;
    @Mock TeamRepository teamRepository;
    @Mock com.league.sportsleague.domain.repository.ChronicleRepository chronicleRepository;
    @InjectMocks FixtureService fixtureService;

    private League league;
    private Season season;
    private Round round;
    private Team home;
    private Team away;
    private Fixture fixture;

    @BeforeEach
    void setUp() {
        league  = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        season  = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
        round   = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(3), RoundStatus.SCHEDULED);
        home    = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
        away    = new Team("Hajduk", "HAJ", "Poljud", "Split", 1911);
        fixture = new Fixture(round, home, away, Instant.now());
    }

    // ── recordResult ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("recordResult calls fixture.recordResult(Score) and saves")
    void recordResultCallsDomainMethodAndSaves() {
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.recordResult(id, new RecordResultRequest(2, 1));

        assertEquals(FixtureStatus.FINISHED, fixture.getStatus());
        assertEquals(2, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
        verify(fixtureRepository).save(fixture);
    }

    @Test
    @DisplayName("recordResult throws EntityNotFoundException when fixture not found")
    void recordResultThrowsWhenNotFound() {
        when(fixtureRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class,
                () -> fixtureService.recordResult(UUID.randomUUID(), new RecordResultRequest(1, 0)));
    }

    @Test
    @DisplayName("recordResult discards stored AI chronicles of the fixture (they would be stale)")
    void recordResultDiscardsChronicles() {
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.recordResult(id, new RecordResultRequest(1, 0));

        verify(chronicleRepository).deleteByFixtureId(id);
    }

    @Test
    @DisplayName("correctResult discards stored AI chronicles of the fixture")
    void correctResultDiscardsChronicles() {
        UUID id = fixture.getId();
        fixture.recordResult(new com.league.sportsleague.domain.valueobject.Score(1, 0));
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.correctResult(id,
                new com.league.sportsleague.application.dto.CorrectResultRequest(2, 1, "VAR"), "admin");

        verify(chronicleRepository).deleteByFixtureId(id);
    }

    // ── correctResult ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("correctResult creates audit and updates score")
    void correctResultCreatesAuditAndUpdatesScore() {
        fixture.recordResult(new Score(2, 1));
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.correctResult(id,
                new CorrectResultRequest(2, 2, "VAR review"), "Referee");

        assertEquals(2, fixture.getHomeScore());
        assertEquals(2, fixture.getAwayScore());
        assertEquals(1, fixture.getAudits().size());
        assertEquals("Referee", fixture.getAudits().get(0).getCorrectedBy());
        verify(fixtureRepository).save(fixture);
    }

    // ── postponeFixture ───────────────────────────────────────────────────────

    @Test
    @DisplayName("postponeFixture calls fixture.postpone() and saves")
    void postponeFixtureCallsDomainMethodAndSaves() {
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.postponeFixture(id, "Rain");

        assertEquals(FixtureStatus.POSTPONED, fixture.getStatus());
        assertEquals("Rain", fixture.getPostponementReason());
        verify(fixtureRepository).save(fixture);
    }

    // ── cancelFixture ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("cancelFixture calls fixture.cancel() and saves")
    void cancelFixtureCallsDomainMethodAndSaves() {
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.cancelFixture(id);

        assertEquals(FixtureStatus.CANCELLED, fixture.getStatus());
        verify(fixtureRepository).save(fixture);
    }

    // ── rescheduleFixture ─────────────────────────────────────────────────────

    @Test
    @DisplayName("rescheduleFixture calls fixture.reschedule() after postpone")
    void rescheduleFixtureCallsDomainMethod() {
        fixture.postpone("Weather");
        UUID id = fixture.getId();
        Instant newTime = Instant.now().plusSeconds(86400);
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.rescheduleFixture(id, newTime);

        assertEquals(FixtureStatus.SCHEDULED, fixture.getStatus());
        assertEquals(newTime, fixture.getScheduledTime());
        verify(fixtureRepository).save(fixture);
    }

    // ── recordMatchEvent ──────────────────────────────────────────────────────

    @Test
    @DisplayName("recordMatchEvent adds event to fixture and saves, returns event UUID")
    void recordMatchEventAddsEventAndSaves() {
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.GOAL, 23, "Bruno Petkovic", null, null);
        UUID eventId = fixtureService.recordMatchEvent(id, req);

        assertNotNull(eventId);
        assertEquals(1, fixture.getEvents().size());
        assertEquals("Bruno Petkovic", fixture.getEvents().get(0).getPlayerName());
        assertEquals(1, fixture.getHomeScore());
        assertEquals(0, fixture.getAwayScore());
        verify(fixtureRepository).save(fixture);
    }

    @Test
    @DisplayName("recordMatchEvent throws EntityNotFoundException when team not found")
    void recordMatchEventThrowsWhenTeamNotFound() {
        UUID id = fixture.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(any())).thenReturn(Optional.empty());

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                UUID.randomUUID(), MatchEventType.GOAL, 23, "Player", null, null);
        assertThrows(EntityNotFoundException.class,
                () -> fixtureService.recordMatchEvent(id, req));
    }

    // ── deleteMatchEvent ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMatchEvent removes the event, saves and discards stale chronicles")
    void deleteMatchEventRemovesEvent() {
        MatchEvent goal = new MatchEvent(fixture, home, MatchEventType.GOAL, 23, "Bruno Petkovic");
        fixture.addMatchEvent(goal);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.deleteMatchEvent(fixture.getId(), goal.getId());

        assertTrue(fixture.getEvents().isEmpty());
        assertEquals(0, fixture.getHomeScore());
        assertEquals(0, fixture.getAwayScore());
        verify(fixtureRepository).save(fixture);
        verify(chronicleRepository).deleteByFixtureId(fixture.getId());
    }

    @Test
    @DisplayName("deleteMatchEvent throws EntityNotFoundException for an event of another fixture")
    void deleteMatchEventThrowsWhenEventNotInFixture() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        assertThrows(EntityNotFoundException.class,
                () -> fixtureService.deleteMatchEvent(fixture.getId(), UUID.randomUUID()));
        verify(fixtureRepository, never()).save(any());
    }

    @Test
    @DisplayName("recordMatchEvent with OWN_GOAL increments opponent score")
    void ownGoalIncrementsOpponentScore() {
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.OWN_GOAL, 12, "Defender", null, null);
        fixtureService.recordMatchEvent(id, req);

        assertEquals(0, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
    }

    @Test
    @DisplayName("recordMatchEvent with non-goal event does not change fixture score")
    void nonGoalEventDoesNotChangeScore() {
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.YELLOW_CARD, 40, "Midfielder", null, null);
        fixtureService.recordMatchEvent(id, req);

        assertNull(fixture.getHomeScore());
        assertNull(fixture.getAwayScore());
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findById returns mapped FixtureResponse")
    void findByIdReturnsResponse() {
        UUID id = fixture.getId();
        fixture.recordResult(new Score(1, 0));
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));

        FixtureResponse response = fixtureService.findById(id);

        assertEquals(id, response.id());
        assertEquals("DIN", response.homeTeamCode());
        assertEquals("HAJ", response.awayTeamCode());
        assertEquals("FINISHED", response.status());
    }

    @Test
    @DisplayName("findById throws EntityNotFoundException when not found")
    void findByIdThrowsWhenNotFound() {
        when(fixtureRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(EntityNotFoundException.class, () -> fixtureService.findById(UUID.randomUUID()));
    }

    // ── findByRound ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByRound returns list of FixtureResponse")
    void findByRoundReturnsResponses() {
        fixture.recordResult(new Score(2, 0));
        when(fixtureRepository.findByRoundId(round.getId())).thenReturn(List.of(fixture));

        List<FixtureResponse> results = fixtureService.findByRound(round.getId());

        assertEquals(1, results.size());
        assertEquals("FINISHED", results.get(0).status());
    }

    // ── findBySeasonWithTeams ─────────────────────────────────────────────────

    @Test
    @DisplayName("findBySeasonWithTeams returns list of FixtureResponse for finished fixtures")
    void findBySeasonWithTeamsReturnsResponses() {
        fixture.recordResult(new Score(1, 1));
        when(fixtureRepository.findFinishedBySeasonWithTeams(season.getId()))
                .thenReturn(List.of(fixture));

        List<FixtureResponse> results = fixtureService.findBySeasonWithTeams(season.getId());

        assertEquals(1, results.size());
        assertEquals("FINISHED", results.get(0).status());
    }
}
