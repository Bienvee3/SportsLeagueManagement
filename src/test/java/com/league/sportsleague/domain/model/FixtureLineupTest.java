package com.league.sportsleague.domain.model;

import com.league.sportsleague.domain.exception.InvalidStateTransitionException;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.model.enums.LineupRole;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Fixture lineups (match sheet)")
class FixtureLineupTest {

    private Team home;
    private Team away;
    private Fixture fixture;

    private List<Player> createPlayers(Team team, int count) {
        List<Player> list = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            list.add(team.addPlayer(team.getShortCode().value() + " Player " + i, i, i == 1 ? "Goalkeeper" : "Player"));
        }
        return list;
    }

    @BeforeEach
    void setUp() {
        League league = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
        Round round = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now().plusDays(2), RoundStatus.SCHEDULED);
        home = new Team("GNK Dinamo Zagreb", "DIN", "Maksimir", "Zagreb", 1911);
        away = new Team("HNK Hajduk Split", "HAJ", "Poljud", "Split", 1911);
        fixture = new Fixture(round, home, away, Instant.now());
    }

    @Nested
    @DisplayName("Lineup submission")
    class Submission {

        @Test
        @DisplayName("valid football lineup (11 starters, 3 bench, captain) is stored with roles")
        void validFootballLineup() {
            List<Player> squad = createPlayers(home, 14);
            List<Player> starters = squad.subList(0, 11);
            List<Player> bench = squad.subList(11, 14);
            Player captain = starters.get(9); // player 10

            fixture.submitLineup(home, starters, bench, captain);

            assertTrue(fixture.hasLineup(home));
            assertFalse(fixture.hasLineup(away));
            List<LineupEntry> entries = fixture.getLineup(home);
            assertEquals(14, entries.size());
            assertEquals(11, entries.stream().filter(LineupEntry::isStarter).count());
            assertEquals(3, entries.stream().filter(e -> !e.isStarter()).count());
            LineupEntry captainEntry = entries.stream().filter(LineupEntry::isCaptain).findFirst().orElseThrow();
            assertEquals(captain.getId(), captainEntry.getPlayer().getId());
        }

        @Test
        @DisplayName("basketball fixture requires exactly 5 starters and accepts up to 7 on the bench")
        void basketballSizesFollowSport() {
            Team bHome = new Team("Cibona", "CIB", "BASKETBALL", "Hall", "Zagreb", 1946);
            Team bAway = new Team("Zadar", "ZAD", "BASKETBALL", "Jazine", "Zadar", 1930);
            Fixture bFixture = new Fixture(fixture.getRound(), bHome, bAway, Instant.now());
            List<Player> squad = createPlayers(bHome, 12);

            bFixture.submitLineup(bHome, squad.subList(0, 5), squad.subList(5, 12), null);

            assertEquals(12, bFixture.getLineup(bHome).size());
            assertEquals(5, bFixture.getLineup(bHome).stream().filter(LineupEntry::isStarter).count());
        }

        @Test
        @DisplayName("submitting fewer or more starters than the sport requires is rejected")
        void wrongStarterCountRejected() {
            List<Player> squad = createPlayers(home, 14);

            IllegalArgumentException tooFew = assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, squad.subList(0, 10), squad.subList(10, 14), null));
            assertEquals("Exactly 11 starters are required, got 10", tooFew.getMessage());

            IllegalArgumentException tooMany = assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, squad.subList(0, 12), squad.subList(12, 14), null));
            assertEquals("Exactly 11 starters are required, got 12", tooMany.getMessage());
        }

        @Test
        @DisplayName("submitting more substitutes than the sport allows is rejected")
        void tooManySubstitutesRejected() {
            List<Player> squad = createPlayers(home, 24);

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 24), null));
        }

        @Test
        @DisplayName("a player listed as both starter and substitute is rejected")
        void duplicatePlayerRejected() {
            List<Player> squad = createPlayers(home, 12);
            List<Player> starters = squad.subList(0, 11);
            List<Player> bench = List.of(starters.get(0)); // player 1 listed twice

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, starters, bench, null));
        }

        @Test
        @DisplayName("a player of another team is rejected")
        void playerOfAnotherTeamRejected() {
            List<Player> homeSquad = createPlayers(home, 11);
            Player foreign = away.addPlayer("Marko Livaja", 10, "Forward");

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, homeSquad, List.of(foreign), null));
        }

        @Test
        @DisplayName("a removed (inactive) player is rejected")
        void removedPlayerRejected() {
            List<Player> squad = createPlayers(home, 14);
            home.removePlayer(squad.get(13));

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 14), null));
        }

        @Test
        @DisplayName("a captain chosen from the bench is rejected")
        void benchCaptainRejected() {
            List<Player> squad = createPlayers(home, 14);

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 14), squad.get(11)));
        }

        @Test
        @DisplayName("submitting for a team that does not play in the fixture is rejected")
        void foreignTeamRejected() {
            Team outsider = new Team("Rijeka", "RIJ", "Rujevica", "Rijeka", 1906);
            List<Player> squad = createPlayers(outsider, 11);

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.submitLineup(outsider, squad, List.of(), null));
        }
    }

    @Nested
    @DisplayName("Diff and replacement")
    class DiffAndClear {

        @Test
        @DisplayName("resubmission updates roles, drops removed players and adds new ones in place")
        void resubmissionDiffsLineup() {
            List<Player> squad = createPlayers(home, 15);
            fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 14), squad.get(0));

            // Swap player 11 (bench) into starters in place of player 1; drop player 14; add player 15
            List<Player> newStarters = new ArrayList<>(squad.subList(1, 11));
            newStarters.add(squad.get(11));
            List<Player> newBench = List.of(squad.get(0), squad.get(12), squad.get(14));
            Player newCaptain = squad.get(10);

            fixture.submitLineup(home, newStarters, newBench, newCaptain);

            List<LineupEntry> entries = fixture.getLineup(home);
            assertEquals(14, entries.size());
            LineupEntry oldStarterNowBench = entries.stream()
                    .filter(e -> e.getPlayer().getId().equals(squad.get(0).getId())).findFirst().orElseThrow();
            assertFalse(oldStarterNowBench.isStarter());
            assertFalse(oldStarterNowBench.isCaptain());

            LineupEntry newCaptainEntry = entries.stream()
                    .filter(e -> e.getPlayer().getId().equals(newCaptain.getId())).findFirst().orElseThrow();
            assertTrue(newCaptainEntry.isCaptain());

            boolean droppedIsGone = entries.stream().noneMatch(e -> e.getPlayer().getId().equals(squad.get(13).getId()));
            assertTrue(droppedIsGone);
        }

        @Test
        @DisplayName("clearLineup withdraws only the entries of the given team")
        void clearLineupEmptiesOnlyGivenTeam() {
            List<Player> homeSquad = createPlayers(home, 11);
            List<Player> awaySquad = createPlayers(away, 11);
            fixture.submitLineup(home, homeSquad, List.of(), null);
            fixture.submitLineup(away, awaySquad, List.of(), null);

            fixture.clearLineup(home);

            assertFalse(fixture.hasLineup(home));
            assertTrue(fixture.hasLineup(away));
            assertEquals(11, fixture.getLineup(away).size());
        }

        @Test
        @DisplayName("lineup cannot be submitted or cleared once the match has started")
        void lineupLockedAfterKickOff() {
            List<Player> squad = createPlayers(home, 11);
            fixture.submitLineup(home, squad, List.of(), null);
            fixture.kickOff();

            assertThrows(InvalidStateTransitionException.class,
                    () -> fixture.submitLineup(home, squad, List.of(), null));
            assertThrows(InvalidStateTransitionException.class,
                    () -> fixture.clearLineup(home));
        }

        @Test
        @DisplayName("lineup cannot change once player-linked events exist for the team")
        void lineupCannotChangeWhenEventsExist() {
            List<Player> squad = createPlayers(home, 11);
            fixture.submitLineup(home, squad, List.of(), null);
            // Move through kickOff to add an event, then test that the event check guards the method
            // (even if a fixture were rescheduled back to SCHEDULED)
            fixture.kickOff();
            fixture.addMatchEvent(new MatchEvent(fixture, home, MatchEventType.GOAL, 10, squad.get(0), null, null));
            org.springframework.test.util.ReflectionTestUtils.setField(fixture, "status", FixtureStatus.SCHEDULED);

            assertThrows(IllegalStateException.class,
                    () -> fixture.submitLineup(home, squad, List.of(), null));
            assertThrows(IllegalStateException.class,
                    () -> fixture.clearLineup(home));
        }
    }

    @Nested
    @DisplayName("Lineup enforcement on match events")
    class EventEnforcement {

        @Test
        @DisplayName("a team with a lineup requires events to reference a player from that lineup")
        void eventRequiresLineupPlayer() {
            List<Player> squad = createPlayers(home, 11);
            fixture.submitLineup(home, squad, List.of(), null);
            fixture.kickOff();

            // Text-only event rejected
            assertThrows(IllegalArgumentException.class,
                    () -> fixture.addMatchEvent(new MatchEvent(fixture, home, MatchEventType.GOAL, 10, "Text Scorer")));

            // Player from the lineup accepted
            MatchEvent ok = new MatchEvent(fixture, home, MatchEventType.GOAL, 10, squad.get(0), null, null);
            fixture.addMatchEvent(ok);
            assertEquals(1, fixture.getEvents().size());
            assertEquals(1, fixture.getHomeScore());
        }

        @Test
        @DisplayName("an event for a player not in the lineup is rejected")
        void playerNotInLineupRejected() {
            List<Player> squad = createPlayers(home, 12);
            fixture.submitLineup(home, squad.subList(0, 11), List.of(), null);
            fixture.kickOff();
            Player omitted = squad.get(11);

            assertThrows(IllegalArgumentException.class,
                    () -> fixture.addMatchEvent(new MatchEvent(fixture, home, MatchEventType.YELLOW_CARD, 20, omitted, null, null)));
        }

        @Test
        @DisplayName("a team WITHOUT a lineup still accepts free-text player names")
        void teamWithoutLineupAcceptsTextEvent() {
            fixture.kickOff();

            fixture.addMatchEvent(new MatchEvent(fixture, home, MatchEventType.GOAL, 10, "Bruno Petkovic"));

            assertEquals(1, fixture.getEvents().size());
            assertEquals(1, fixture.getHomeScore());
        }
    }

    @Nested
    @DisplayName("Substitutions against the lineup")
    class Substitutions {

        private List<Player> squad;

        @BeforeEach
        void startMatch() {
            squad = createPlayers(home, 14); // #1-#11 start, #12-#14 on the bench
            fixture.submitLineup(home, squad.subList(0, 11), squad.subList(11, 14), null);
            fixture.kickOff();
        }

        private MatchEvent substitution(int minute, int off, int on) {
            return new MatchEvent(fixture, home, MatchEventType.SUBSTITUTION, minute,
                    squad.get(off - 1), squad.get(on - 1), null);
        }

        @Test
        @DisplayName("a valid substitution is recorded")
        void validSubstitutionRecorded() {
            fixture.addMatchEvent(substitution(60, 9, 12));

            assertEquals(1, fixture.getEvents().size());
            assertEquals(60, fixture.replayPitch(home).minutesPlayed(squad.get(8).getId(), 90));
        }

        @Test
        @DisplayName("an impossible substitution is rejected and leaves the events unchanged")
        void invalidSubstitutionLeavesEventsUnchanged() {
            MatchEvent first = substitution(60, 9, 12);
            fixture.addMatchEvent(first);
            MatchEvent impossible = substitution(70, 10, 12); // #12 is already on the pitch

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fixture.addMatchEvent(impossible));

            assertEquals("DIN Player 12 is already on the pitch at minute 70", ex.getMessage());
            assertEquals(List.of(first), fixture.getEvents());
            assertNull(impossible.getFixture());
        }

        @Test
        @DisplayName("deleting a substitution that a later one depends on is refused and keeps the event")
        void dependentSubstitutionCannotBeDeleted() {
            MatchEvent on = substitution(60, 9, 12);
            MatchEvent offAgain = substitution(75, 12, 13); // needs #12 on the pitch
            fixture.addMatchEvent(on);
            fixture.addMatchEvent(offAgain);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> fixture.removeMatchEvent(on));

            assertTrue(ex.getMessage().startsWith("The event cannot be deleted because later events depend on it: "));
            assertEquals(List.of(on, offAgain), fixture.getEvents());
            assertSame(fixture, on.getFixture());
        }

        @Test
        @DisplayName("the last substitution can be deleted")
        void lastSubstitutionCanBeDeleted() {
            MatchEvent on = substitution(60, 9, 12);
            MatchEvent offAgain = substitution(75, 12, 13);
            fixture.addMatchEvent(on);
            fixture.addMatchEvent(offAgain);

            fixture.removeMatchEvent(offAgain);

            assertEquals(List.of(on), fixture.getEvents());
            assertNull(offAgain.getFixture());
        }
    }
}
