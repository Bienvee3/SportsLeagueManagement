package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.LineupEntry;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.model.enums.RoundStatus;
import com.league.sportsleague.domain.model.enums.SeasonStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PitchTimeline")
class PitchTimelineTest {

    /** A team of the given sport with players #1..#squadSize; the first {@code starters} start, the rest sit on the bench. */
    private static final class Side {
        final Team team;
        final Fixture fixture;
        final List<Player> players = new ArrayList<>();
        final List<MatchEvent> events = new ArrayList<>();
        final LineupRules rules;

        Side(String sport, int squadSize) {
            League league = new League("League", "league", sport, "Croatia");
            Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
            Round round = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now(), RoundStatus.SCHEDULED);
            team = new Team("Home", "HOM", sport, "Stadium", "City", 2000);
            Team away = new Team("Away", "AWY", sport, "Stadium", "City", 2000);
            fixture = new Fixture(round, team, away, Instant.now());
            rules = LineupRules.forSport(sport);
            for (int n = 1; n <= squadSize; n++) {
                players.add(team.addPlayer("P" + n, n, null));
            }
            fixture.submitLineup(team, players.subList(0, rules.starters()),
                    players.subList(rules.starters(), squadSize), null);
        }

        /** Player with shirt number n. */
        Player no(int n) {
            return players.get(n - 1);
        }

        Side sub(int minute, int off, int on) {
            events.add(new MatchEvent(fixture, team, MatchEventType.SUBSTITUTION, minute, no(off), no(on), null));
            return this;
        }

        Side red(int minute, int player) {
            events.add(new MatchEvent(fixture, team, MatchEventType.RED_CARD, minute, no(player), null, null));
            return this;
        }

        PitchTimeline replay() {
            List<LineupEntry> lineup = fixture.getLineup(team);
            return PitchTimeline.replay(lineup, events, rules);
        }
    }

    private static IllegalArgumentException rejected(Side side) {
        return assertThrows(IllegalArgumentException.class, side::replay);
    }

    @Nested
    @DisplayName("Substitution checks")
    class SubstitutionChecks {

        @Test
        @DisplayName("the player going off must be on the pitch")
        void offPlayerMustBeOnPitch() {
            Side s = new Side("FOOTBALL", 16).sub(60, 12, 13); // #12 is on the bench

            assertEquals("P12 is not on the pitch at minute 60", rejected(s).getMessage());
        }

        @Test
        @DisplayName("the player coming on must not already be on the pitch")
        void onPlayerMustNotBeOnPitch() {
            Side s = new Side("FOOTBALL", 16).sub(60, 9, 12).sub(70, 10, 12);

            assertEquals("P12 is already on the pitch at minute 70", rejected(s).getMessage());
        }

        @Test
        @DisplayName("a substitution needs the player coming on")
        void onPlayerRequired() {
            Side s = new Side("FOOTBALL", 16);
            s.events.add(new MatchEvent(s.fixture, s.team, MatchEventType.SUBSTITUTION, 60, s.no(9), null, null));

            assertEquals("A substitution needs the player coming on", rejected(s).getMessage());
        }

        @Test
        @DisplayName("football: a replaced player cannot come back on")
        void footballRejectsReentry() {
            Side s = new Side("FOOTBALL", 16).sub(60, 9, 12).sub(75, 12, 9);

            assertEquals("P9 has already played in this match and cannot re-enter", rejected(s).getMessage());
        }

        @Test
        @DisplayName("basketball: a replaced player may come back on")
        void basketballAllowsReentry() {
            Side s = new Side("BASKETBALL", 12).sub(10, 5, 6).sub(20, 6, 5);

            assertEquals(Set.of(1, 2, 3, 4, 5, 6),
                    Set.copyOf(s.replay().appearedPlayerIds().stream().map(id -> s.team.findPlayer(id).orElseThrow().getShirtNumber()).toList()));
        }

        @Test
        @DisplayName("football: the sixth substitution is over the limit of five")
        void footballLimitsSubstitutions() {
            Side s = new Side("FOOTBALL", 17)
                    .sub(46, 2, 12).sub(55, 3, 13).sub(60, 4, 14).sub(70, 5, 15).sub(80, 6, 16);
            assertDoesNotThrow(s::replay);

            s.sub(85, 7, 17);

            assertEquals("Substitution limit reached (5)", rejected(s).getMessage());
        }

        @Test
        @DisplayName("basketball: ten substitutions are allowed")
        void basketballHasNoSubstitutionLimit() {
            Side s = new Side("BASKETBALL", 12);
            for (int i = 0; i < 5; i++) {
                s.sub(4 * i + 1, 1, 6).sub(4 * i + 2, 6, 1);
            }

            assertDoesNotThrow(s::replay);
        }

        @Test
        @DisplayName("a sent-off player cannot come on, even in a sport with re-entry")
        void sentOffPlayerCannotComeOn() {
            Side s = new Side("BASKETBALL", 12).sub(10, 5, 6).red(15, 5).sub(20, 6, 5);

            assertEquals("P5 was sent off and cannot come on", rejected(s).getMessage());
        }

        @Test
        @DisplayName("a player cannot be sent off twice")
        void secondRedCardRejected() {
            Side s = new Side("FOOTBALL", 16).red(30, 4).red(50, 4);

            assertEquals("P4 has already been sent off", rejected(s).getMessage());
        }

        @Test
        @DisplayName("events are replayed by minute, not in recording order")
        void replayedInMinuteOrder() {
            // Recorded out of order: #12 for #9 at 60 is valid before #13 for #12 at 70.
            Side valid = new Side("FOOTBALL", 16).sub(70, 12, 13).sub(60, 9, 12);
            assertDoesNotThrow(valid::replay);

            // #12 cannot go off at 50 when he only comes on at 60.
            Side invalid = new Side("FOOTBALL", 16).sub(60, 9, 12).sub(50, 12, 13);
            assertEquals("P12 is not on the pitch at minute 50", rejected(invalid).getMessage());
        }
    }

    @Nested
    @DisplayName("Minutes played")
    class MinutesPlayed {

        @Test
        @DisplayName("minutes and appearances over a 90-minute football match")
        void minutesOverNinety() {
            Side s = new Side("FOOTBALL", 16).sub(60, 9, 12).red(30, 4);

            PitchTimeline t = s.replay();

            assertEquals(60, t.minutesPlayed(s.no(9).getId(), 90));   // replaced at 60
            assertEquals(30, t.minutesPlayed(s.no(12).getId(), 90));  // on at 60
            assertEquals(30, t.minutesPlayed(s.no(4).getId(), 90));   // sent off at 30
            assertEquals(90, t.minutesPlayed(s.no(1).getId(), 90));   // untouched starter
            assertEquals(0, t.minutesPlayed(s.no(13).getId(), 90));   // unused substitute
            assertTrue(t.appearedPlayerIds().contains(s.no(12).getId()));
            assertFalse(t.appearedPlayerIds().contains(s.no(13).getId()));
            assertEquals(12, t.appearedPlayerIds().size());
        }

        @Test
        @DisplayName("a basketball player who leaves and returns sums both spells")
        void reentrySumsSpells() {
            Side s = new Side("BASKETBALL", 12).sub(10, 5, 6).sub(25, 6, 5);

            PitchTimeline t = s.replay();

            assertEquals(10 + 15, t.minutesPlayed(s.no(5).getId(), 40));
            assertEquals(15, t.minutesPlayed(s.no(6).getId(), 40));
        }
    }
}
