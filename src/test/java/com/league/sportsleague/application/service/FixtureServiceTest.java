package com.league.sportsleague.application.service;

import com.league.sportsleague.application.dto.CorrectResultRequest;
import com.league.sportsleague.application.dto.FixtureResponse;
import com.league.sportsleague.application.dto.MatchSheetResponse;
import com.league.sportsleague.application.dto.RecordMatchEventRequest;
import com.league.sportsleague.application.dto.RecordResultRequest;
import com.league.sportsleague.application.dto.SubmitLineupRequest;
import com.league.sportsleague.domain.exception.EntityNotFoundException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
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
        fixture.kickOff();
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.GOAL, 23, "Bruno Petkovic", null, null, null, null);
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
                UUID.randomUUID(), MatchEventType.GOAL, 23, "Player", null, null, null, null);
        assertThrows(EntityNotFoundException.class,
                () -> fixtureService.recordMatchEvent(id, req));
    }

    // ── deleteMatchEvent ──────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteMatchEvent removes the event, saves and discards stale chronicles")
    void deleteMatchEventRemovesEvent() {
        fixture.kickOff();
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
        fixture.kickOff();
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.OWN_GOAL, 12, "Defender", null, null, null, null);
        fixtureService.recordMatchEvent(id, req);

        assertEquals(0, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
    }

    @Test
    @DisplayName("recordMatchEvent with non-goal event does not change fixture score")
    void nonGoalEventDoesNotChangeScore() {
        fixture.kickOff();
        UUID id = fixture.getId();
        UUID teamId = home.getId();
        when(fixtureRepository.findById(id)).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                teamId, MatchEventType.YELLOW_CARD, 40, "Midfielder", null, null, null, null);
        fixtureService.recordMatchEvent(id, req);

        assertEquals(0, fixture.getHomeScore());
        assertEquals(0, fixture.getAwayScore());
    }

    @Test
    @DisplayName("recordMatchEvent rejects a goal on a FINISHED fixture: score kept, nothing saved, chronicles kept")
    void recordGoalOnFinishedFixtureIsRejected() {
        fixture.recordResult(new Score(2, 1));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(home.getId())).thenReturn(Optional.of(home));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                home.getId(), MatchEventType.GOAL, 90, "Striker", null, null, null, null);

        assertThrows(InvalidStateTransitionException.class,
                () -> fixtureService.recordMatchEvent(fixture.getId(), req));

        assertEquals(2, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
        assertTrue(fixture.getEvents().isEmpty());
        verify(fixtureRepository, never()).save(any());
        verify(chronicleRepository, never()).deleteByFixtureId(any());
    }

    @Test
    @DisplayName("recordMatchEvent still accepts a card on a FINISHED fixture and discards stale chronicles")
    void recordCardOnFinishedFixtureIsAllowed() {
        fixture.recordResult(new Score(2, 1));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(away.getId())).thenReturn(Optional.of(away));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.recordMatchEvent(fixture.getId(), new RecordMatchEventRequest(
                away.getId(), MatchEventType.YELLOW_CARD, 70, "Defender", null, null, null, null));

        assertEquals(1, fixture.getEvents().size());
        assertEquals(2, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
        verify(chronicleRepository).deleteByFixtureId(fixture.getId());
    }

    @Test
    @DisplayName("recordMatchEvent rejects an event whose team is neither home nor away")
    void recordEventOfForeignTeamIsRejected() {
        fixture.kickOff();
        Team outsider = new Team("Rijeka", "RIJ", "Rujevica", "Rijeka", 1906);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                outsider.getId(), MatchEventType.GOAL, 12, "Outsider", null, null, null, null);

        assertThrows(IllegalArgumentException.class,
                () -> fixtureService.recordMatchEvent(fixture.getId(), req));

        assertTrue(fixture.getEvents().isEmpty());
        verify(fixtureRepository, never()).save(any());
        verify(chronicleRepository, never()).deleteByFixtureId(any());
    }

    @Test
    @DisplayName("deleteMatchEvent rejects removing a goal from a FINISHED fixture: event and score kept")
    void deleteGoalOnFinishedFixtureIsRejected() {
        fixture.kickOff();
        MatchEvent goal = new MatchEvent(fixture, home, MatchEventType.GOAL, 23, "Bruno Petkovic");
        fixture.addMatchEvent(goal);
        fixture.recordResult(new Score(1, 0));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        assertThrows(InvalidStateTransitionException.class,
                () -> fixtureService.deleteMatchEvent(fixture.getId(), goal.getId()));

        assertEquals(1, fixture.getEvents().size());
        assertEquals(1, fixture.getHomeScore());
        verify(fixtureRepository, never()).save(any());
        verify(chronicleRepository, never()).deleteByFixtureId(any());
    }

    @Test
    @DisplayName("a score set by correctResult is not overwritten by a later event")
    void correctedScoreIsNotOverwrittenByLaterEvents() {
        fixture.recordResult(new Score(1, 0));
        fixture.correctResult(new Score(3, 0), "Forfeit ruling", "admin");
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(away.getId())).thenReturn(Optional.of(away));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        fixtureService.recordMatchEvent(fixture.getId(), new RecordMatchEventRequest(
                away.getId(), MatchEventType.RED_CARD, 80, "Defender", null, null, null, null));

        assertEquals(3, fixture.getHomeScore());
        assertEquals(0, fixture.getAwayScore());
    }

    // ── kickOff / finish ──────────────────────────────────────────────────────

    @Test
    @DisplayName("kickOff moves the fixture to IN_PROGRESS at 0-0 and saves")
    void kickOffStartsMatchAndSaves() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        fixtureService.kickOff(fixture.getId());

        assertEquals(FixtureStatus.IN_PROGRESS, fixture.getStatus());
        assertEquals(0, fixture.getHomeScore());
        assertEquals(0, fixture.getAwayScore());
        verify(fixtureRepository).save(fixture);
    }

    @Test
    @DisplayName("kickOff on a match already in progress is rejected and nothing is saved")
    void kickOffTwiceIsRejected() {
        fixture.kickOff();
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        assertThrows(InvalidStateTransitionException.class, () -> fixtureService.kickOff(fixture.getId()));

        verify(fixtureRepository, never()).save(any());
    }

    @Test
    @DisplayName("finish ends the match with the event score, saves and discards stale chronicles")
    void finishEndsMatchAndDiscardsChronicles() {
        fixture.kickOff();
        fixture.addMatchEvent(new MatchEvent(fixture, away, MatchEventType.GOAL, 12, "Marko Livaja"));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        fixtureService.finish(fixture.getId());

        assertEquals(FixtureStatus.FINISHED, fixture.getStatus());
        assertEquals(0, fixture.getHomeScore());
        assertEquals(1, fixture.getAwayScore());
        verify(fixtureRepository).save(fixture);
        verify(chronicleRepository).deleteByFixtureId(fixture.getId());
    }

    @Test
    @DisplayName("finish before kick-off is rejected: nothing saved, chronicles kept")
    void finishBeforeKickOffIsRejected() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        assertThrows(InvalidStateTransitionException.class, () -> fixtureService.finish(fixture.getId()));

        assertEquals(FixtureStatus.SCHEDULED, fixture.getStatus());
        verify(fixtureRepository, never()).save(any());
        verify(chronicleRepository, never()).deleteByFixtureId(any());
    }

    @Test
    @DisplayName("recordMatchEvent before kick-off is rejected and nothing is saved")
    void recordEventBeforeKickOffIsRejected() {
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(home.getId())).thenReturn(Optional.of(home));

        assertThrows(InvalidStateTransitionException.class,
                () -> fixtureService.recordMatchEvent(fixture.getId(), new RecordMatchEventRequest(
                        home.getId(), MatchEventType.YELLOW_CARD, 5, "Midfielder", null, null, null, null)));

        assertTrue(fixture.getEvents().isEmpty());
        verify(fixtureRepository, never()).save(any());
    }

    // ── submitLineup / clearLineup / getMatchSheet ────────────────────────────

    private List<Player> populateSquad(Team team, int count) {
        List<Player> list = new java.util.ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(team.addPlayer(team.getShortCode().value() + " P" + i, i, "Pos"));
        }
        return list;
    }

    @Test
    @DisplayName("submitLineup resolves player ids against the team, submits and saves")
    void submitLineupResolvesAndSaves() {
        List<Player> squad = populateSquad(home, 14);
        List<UUID> starters = squad.subList(0, 11).stream().map(Player::getId).toList();
        List<UUID> bench = squad.subList(11, 14).stream().map(Player::getId).toList();
        UUID captain = starters.get(0);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        fixtureService.submitLineup(fixture.getId(), home.getId(), new SubmitLineupRequest(starters, bench, captain));

        assertTrue(fixture.hasLineup(home));
        assertEquals(14, fixture.getLineup(home).size());
        verify(fixtureRepository).save(fixture);
    }

    @Test
    @DisplayName("submitLineup throws EntityNotFoundException when a player id is not in the team")
    void submitLineupThrowsWhenPlayerNotFound() {
        populateSquad(home, 10);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        assertThrows(EntityNotFoundException.class,
                () -> fixtureService.submitLineup(fixture.getId(), home.getId(),
                        new SubmitLineupRequest(List.of(UUID.randomUUID()), List.of(), null)));
    }

    @Test
    @DisplayName("submitLineup rejects a team that does not play in the fixture and saves nothing")
    void submitLineupRejectsForeignTeam() {
        Team outsider = new Team("Rijeka", "RIJ", "Rujevica", "Rijeka", 1906);
        Player foreign = outsider.addPlayer("Outsider", 7, null);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fixtureService.submitLineup(fixture.getId(), outsider.getId(),
                        new SubmitLineupRequest(List.of(foreign.getId()), List.of(), null)));

        assertEquals("Team does not play in this fixture", ex.getMessage());
        assertTrue(fixture.getLineupEntries().isEmpty());
        verify(fixtureRepository, never()).save(any());
    }

    @Test
    @DisplayName("clearLineup clears the lineup of the team and saves")
    void clearLineupClearsAndSaves() {
        List<Player> squad = populateSquad(home, 11);
        fixture.submitLineup(home, squad, List.of(), null);
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        fixtureService.clearLineup(fixture.getId(), home.getId());

        assertFalse(fixture.hasLineup(home));
        verify(fixtureRepository).save(fixture);
    }

    @Test
    @DisplayName("recordMatchEvent with playerId links the squad player and copies its name")
    void recordMatchEventWithPlayerId() {
        Player striker = home.addPlayer("Bruno Petkovic", 9, "Forward");
        Player playmaker = home.addPlayer("Martin Baturina", 10, "Midfielder");
        fixture.kickOff();
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(home.getId())).thenReturn(Optional.of(home));
        when(fixtureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecordMatchEventRequest req = new RecordMatchEventRequest(
                home.getId(), MatchEventType.GOAL, 23, null, null, null, striker.getId(), playmaker.getId());
        fixtureService.recordMatchEvent(fixture.getId(), req);

        assertEquals(1, fixture.getEvents().size());
        MatchEvent event = fixture.getEvents().get(0);
        assertEquals(striker, event.getPlayer());
        assertEquals(playmaker, event.getRelatedPlayer());
        assertEquals("Bruno Petkovic", event.getPlayerName());
        assertEquals("Martin Baturina", event.getAssistingPlayer());
    }

    @Test
    @DisplayName("recordMatchEvent with neither a player id nor a player name is rejected and nothing is saved")
    void recordMatchEventWithoutPlayerIsRejected() {
        fixture.kickOff();
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));
        when(teamRepository.findById(home.getId())).thenReturn(Optional.of(home));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> fixtureService.recordMatchEvent(fixture.getId(), new RecordMatchEventRequest(
                        home.getId(), MatchEventType.YELLOW_CARD, 30, "  ", null, null, null, null)));

        assertEquals("A player must be selected or named", ex.getMessage());
        assertTrue(fixture.getEvents().isEmpty());
        verify(fixtureRepository, never()).save(any());
        verify(chronicleRepository, never()).deleteByFixtureId(any());
    }

    @Test
    @DisplayName("getMatchSheet exposes sport sizes, lineup submission status and squad options")
    void getMatchSheetExposesDetails() {
        List<Player> squad = populateSquad(home, 15);
        fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 14), squad.get(0));
        when(fixtureRepository.findById(fixture.getId())).thenReturn(Optional.of(fixture));

        MatchSheetResponse sheet = fixtureService.getMatchSheet(fixture.getId());

        assertEquals(11, sheet.startersRequired());
        assertEquals(12, sheet.maxBench());
        assertTrue(sheet.home().lineupSubmitted());
        assertFalse(sheet.away().lineupSubmitted());
        assertEquals(11, sheet.home().starters().size());
        assertEquals(3, sheet.home().bench().size());
        assertEquals(15, sheet.home().squad().size());
        assertTrue(sheet.home().starters().get(0).captain());
        // squad options are ordered by shirt number: #1 starts as captain, #12 is on the bench, #15 is not selected
        assertEquals("STARTER", sheet.home().squad().get(0).selectedRole());
        assertTrue(sheet.home().squad().get(0).captain());
        assertEquals("SUBSTITUTE", sheet.home().squad().get(11).selectedRole());
        assertNull(sheet.home().squad().get(14).selectedRole());
        assertTrue(sheet.away().squad().isEmpty());
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
