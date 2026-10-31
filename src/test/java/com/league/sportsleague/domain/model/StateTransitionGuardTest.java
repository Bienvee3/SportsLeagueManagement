package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.DuplicateTeamException;
import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.*;
import com.league.sportsleague.domain.valueobject.Score;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StateTransitionGuardTest {

    // ── helpers ───────────────────────────────────────────────────────────────

    private League league() {
        return new League("SuperSport HNL", "supersport-hnl", "FOOTBALL", "Croatia");
    }

    private Team team(String code) {
        return new Team("Team " + code, code, "Stadium", "City", 2000);
    }

    private Season activeSeason() {
        Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
        s.activate();
        return s;
    }

    private Round scheduledRound(Season season) {
        return new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(3), RoundStatus.SCHEDULED);
    }

    private Fixture scheduledFixture(Round round) {
        Team home = team("HOM");
        Team away = team("AWY");
        return new Fixture(round, home, away, Instant.now());
    }

    // ── Season ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Season state transitions")
    class SeasonGuards {

        @Test
        @DisplayName("activate() throws when already ACTIVE")
        void activateFromActiveThrows() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            s.activate();
            assertThrows(InvalidStateTransitionException.class, s::activate);
        }

        @Test
        @DisplayName("activate() throws when COMPLETED")
        void activateFromCompletedThrows() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            s.activate();
            s.complete();
            assertThrows(InvalidStateTransitionException.class, s::activate);
        }

        @Test
        @DisplayName("complete() throws when UPCOMING")
        void completeFromUpcomingThrows() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            assertThrows(InvalidStateTransitionException.class, s::complete);
        }

        @Test
        @DisplayName("complete() throws when already COMPLETED")
        void completeFromCompletedThrows() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            s.activate();
            s.complete();
            assertThrows(InvalidStateTransitionException.class, s::complete);
        }

        @Test
        @DisplayName("Valid path: UPCOMING -> ACTIVE -> COMPLETED succeeds")
        void validLifecyclePath() {
            Season s = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            s.activate();
            assertEquals(SeasonStatus.ACTIVE, s.getStatus());
            s.complete();
            assertEquals(SeasonStatus.COMPLETED, s.getStatus());
        }
    }

    // ── Round ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round state transitions")
    class RoundGuards {

        @Test
        @DisplayName("startRound() throws when already IN_PROGRESS")
        void startFromInProgressThrows() {
            Round r = scheduledRound(activeSeason());
            r.startRound();
            assertThrows(InvalidStateTransitionException.class, r::startRound);
        }

        @Test
        @DisplayName("startRound() throws when COMPLETED")
        void startFromCompletedThrows() {
            Round r = scheduledRound(activeSeason());
            r.startRound();
            r.completeRound();
            assertThrows(InvalidStateTransitionException.class, r::startRound);
        }

        @Test
        @DisplayName("completeRound() throws when SCHEDULED")
        void completeFromScheduledThrows() {
            Round r = scheduledRound(activeSeason());
            assertThrows(InvalidStateTransitionException.class, r::completeRound);
        }

        @Test
        @DisplayName("completeRound() throws when already COMPLETED")
        void completeFromCompletedThrows() {
            Round r = scheduledRound(activeSeason());
            r.startRound();
            r.completeRound();
            assertThrows(InvalidStateTransitionException.class, r::completeRound);
        }

        @Test
        @DisplayName("Valid path: SCHEDULED -> IN_PROGRESS -> COMPLETED succeeds")
        void validLifecyclePath() {
            Round r = scheduledRound(activeSeason());
            r.startRound();
            assertEquals(RoundStatus.IN_PROGRESS, r.getStatus());
            r.completeRound();
            assertEquals(RoundStatus.COMPLETED, r.getStatus());
        }
    }

    // ── Fixture ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Fixture state transitions")
    class FixtureGuards {

        @Test
        @DisplayName("recordResult() throws when FINISHED")
        void recordResultOnFinishedThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.recordResult(new Score(2, 1));
            assertThrows(InvalidStateTransitionException.class, () -> f.recordResult(new Score(3, 0)));
        }

        @Test
        @DisplayName("recordResult() throws when CANCELLED")
        void recordResultOnCancelledThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.cancel();
            assertThrows(InvalidStateTransitionException.class, () -> f.recordResult(new Score(1, 0)));
        }

        @Test
        @DisplayName("recordResult() throws when POSTPONED")
        void recordResultOnPostponedThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.postpone("Weather");
            assertThrows(InvalidStateTransitionException.class, () -> f.recordResult(new Score(1, 0)));
        }

        @Test
        @DisplayName("postpone() throws when FINISHED")
        void postponeOnFinishedThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.recordResult(new Score(1, 0));
            assertThrows(InvalidStateTransitionException.class, () -> f.postpone("Late reason"));
        }

        @Test
        @DisplayName("postpone() throws when CANCELLED")
        void postponeOnCancelledThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.cancel();
            assertThrows(InvalidStateTransitionException.class, () -> f.postpone("Reason"));
        }

        @Test
        @DisplayName("cancel() throws when FINISHED")
        void cancelOnFinishedThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.recordResult(new Score(1, 0));
            assertThrows(InvalidStateTransitionException.class, f::cancel);
        }

        @Test
        @DisplayName("cancel() throws when already CANCELLED")
        void cancelOnCancelledThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.cancel();
            assertThrows(InvalidStateTransitionException.class, f::cancel);
        }

        @Test
        @DisplayName("reschedule() throws when SCHEDULED (not postponed)")
        void rescheduleOnScheduledThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            assertThrows(InvalidStateTransitionException.class,
                () -> f.reschedule(Instant.now().plusSeconds(3600)));
        }

        @Test
        @DisplayName("reschedule() throws when FINISHED")
        void rescheduleOnFinishedThrows() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.recordResult(new Score(1, 0));
            assertThrows(InvalidStateTransitionException.class,
                () -> f.reschedule(Instant.now().plusSeconds(3600)));
        }

        @Test
        @DisplayName("Valid: SCHEDULED -> POSTPONED -> SCHEDULED succeeds")
        void postponeAndReschedule() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            f.postpone("Rain");
            assertEquals(FixtureStatus.POSTPONED, f.getStatus());
            f.reschedule(Instant.now().plusSeconds(86400));
            assertEquals(FixtureStatus.SCHEDULED, f.getStatus());
            assertNull(f.getPostponementReason());
        }

        @Test
        @DisplayName("Fixture constructor throws DuplicateTeamException when home == away")
        void fixtureRejectsSameHomeAndAway() {
            Round r = scheduledRound(activeSeason());
            Team t = team("DIN");
            assertThrows(DuplicateTeamException.class,
                () -> new Fixture(r, t, t, Instant.now()));
        }
    }

    // ── SeasonTeam ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("SeasonTeam state transitions")
    class SeasonTeamGuards {

        @Test
        @DisplayName("withdraw() throws when already WITHDRAWN")
        void withdrawFromWithdrawnThrows() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            st.withdraw();
            assertThrows(InvalidStateTransitionException.class, st::withdraw);
        }

        @Test
        @DisplayName("withdraw() throws when DISQUALIFIED")
        void withdrawFromDisqualifiedThrows() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            st.disqualify();
            assertThrows(InvalidStateTransitionException.class, st::withdraw);
        }

        @Test
        @DisplayName("disqualify() throws when already DISQUALIFIED")
        void disqualifyFromDisqualifiedThrows() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            st.disqualify();
            assertThrows(InvalidStateTransitionException.class, st::disqualify);
        }

        @Test
        @DisplayName("reinstate() succeeds from WITHDRAWN")
        void reinstateFromWithdrawn() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            st.withdraw();
            st.reinstate();
            assertEquals(SeasonTeamStatus.CONFIRMED, st.getStatus());
        }

        @Test
        @DisplayName("reinstate() throws when CONFIRMED (not withdrawn)")
        void reinstateFromConfirmedThrows() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            assertThrows(InvalidStateTransitionException.class, st::reinstate);
        }

        @Test
        @DisplayName("reinstate() throws when DISQUALIFIED")
        void reinstateFromDisqualifiedThrows() {
            SeasonTeam st = new SeasonTeam(activeSeason(), team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            st.disqualify();
            assertThrows(InvalidStateTransitionException.class, st::reinstate);
        }
    }

    // ── Season.addSeasonTeam duplicate guard ──────────────────────────────────

    @Nested
    @DisplayName("Season.addSeasonTeam duplicate guard")
    class SeasonAddTeamGuard {

        @Test
        @DisplayName("Adding the same team twice throws DuplicateTeamException")
        void addingSameTeamTwiceThrows() {
            Season season = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            Team team = team("DIN");
            SeasonTeam st1 = new SeasonTeam(season, team, 1, SeasonTeamStatus.CONFIRMED);
            SeasonTeam st2 = new SeasonTeam(season, team, 2, SeasonTeamStatus.CONFIRMED);
            season.addSeasonTeam(st1);
            assertThrows(DuplicateTeamException.class, () -> season.addSeasonTeam(st2));
        }

        @Test
        @DisplayName("Adding two different teams succeeds")
        void addingDifferentTeamsSucceeds() {
            Season season = new Season(league(), "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
            SeasonTeam st1 = new SeasonTeam(season, team("DIN"), 1, SeasonTeamStatus.CONFIRMED);
            SeasonTeam st2 = new SeasonTeam(season, team("HAJ"), 2, SeasonTeamStatus.CONFIRMED);
            season.addSeasonTeam(st1);
            season.addSeasonTeam(st2);
            assertEquals(2, season.getSeasonTeams().size());
        }
    }

    // ── Constructor validations ───────────────────────────────────────────────

    @Nested
    @DisplayName("Constructor validations")
    class ConstructorValidations {

        @Test
        @DisplayName("League rejects null name")
        void leagueRejectsNullName() {
            assertThrows(IllegalArgumentException.class,
                () -> new League(null, "slug", "FOOTBALL", "Croatia"));
        }

        @Test
        @DisplayName("League rejects blank name")
        void leagueRejectsBlankName() {
            assertThrows(IllegalArgumentException.class,
                () -> new League("  ", "slug", "FOOTBALL", "Croatia"));
        }

        @Test
        @DisplayName("Team rejects null name")
        void teamRejectsNullName() {
            assertThrows(IllegalArgumentException.class,
                () -> new Team(null, "DIN", "Stadium", "City", 2000));
        }

        @Test
        @DisplayName("Season rejects yearStart >= yearEnd")
        void seasonRejectsInvalidYears() {
            assertThrows(IllegalArgumentException.class,
                () -> new Season(league(), "Test", 2027, 2026, SeasonStatus.UPCOMING));
        }

        @Test
        @DisplayName("Season rejects equal yearStart and yearEnd")
        void seasonRejectsEqualYears() {
            assertThrows(IllegalArgumentException.class,
                () -> new Season(league(), "Test", 2026, 2026, SeasonStatus.UPCOMING));
        }

        @Test
        @DisplayName("Round rejects roundNumber less than 1")
        void roundRejectsZeroRoundNumber() {
            assertThrows(IllegalArgumentException.class,
                () -> new Round(activeSeason(), 0, "Round 0", null, null, RoundStatus.SCHEDULED));
        }

        @Test
        @DisplayName("MatchEvent rejects matchMinute 0 (below MatchMinute range)")
        void matchEventRejectsMinuteZero() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            Team t = team("DIN");
            assertThrows(IllegalArgumentException.class,
                () -> new MatchEvent(f, t, MatchEventType.GOAL, 0, "Player"));
        }

        @Test
        @DisplayName("MatchEvent rejects matchMinute 131 (above MatchMinute range)")
        void matchEventRejectsMinute131() {
            Round r = scheduledRound(activeSeason());
            Fixture f = scheduledFixture(r);
            Team t = team("DIN");
            assertThrows(IllegalArgumentException.class,
                () -> new MatchEvent(f, t, MatchEventType.GOAL, 131, "Player"));
        }
    }
}
