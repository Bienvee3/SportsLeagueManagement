package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.League;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Round;
import com.league.sportsleague.domain.model.Season;
import com.league.sportsleague.domain.model.Team;
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

@DisplayName("PlayerStatsCalculator")
class PlayerStatsCalculatorTest {

    private Round round;
    private Team dinamo;
    private Team hajduk;

    @BeforeEach
    void setUp() {
        League league = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        Season season = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.ACTIVE);
        round = new Round(season, 1, "Round 1", LocalDate.now(), LocalDate.now(), RoundStatus.SCHEDULED);
        dinamo = new Team("Dinamo", "DIN", "Maksimir", "Zagreb", 1911);
        hajduk = new Team("Hajduk", "HAJ", "Poljud", "Split", 1911);
    }

    private Fixture live(Team home, Team away) {
        Fixture f = new Fixture(round, home, away, Instant.now());
        f.kickOff();
        return f;
    }

    private static PlayerStatsEntry row(List<PlayerStatsEntry> stats, String name) {
        return stats.stream().filter(e -> e.playerName().equals(name)).findFirst()
                .orElseThrow(() -> new AssertionError("No row for " + name + " in " + stats));
    }

    @Nested
    @DisplayName("Event counting")
    class EventCounting {

        @Test
        @DisplayName("GOAL and PENALTY count as goals, OWN_GOAL does not")
        void goalsAndPenaltiesCountOwnGoalsDoNot() {
            Player striker = dinamo.addPlayer("Bruno Petkovic", 9, null);
            Player defender = hajduk.addPlayer("Dario Melnjak", 4, null);
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 10, striker, null, null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.PENALTY, 40, striker, null, null));
            f.addMatchEvent(new MatchEvent(f, hajduk, MatchEventType.OWN_GOAL, 60, defender, null, null));
            f.finish();

            List<PlayerStatsEntry> stats = PlayerStatsCalculator.calculate(List.of(f));

            assertEquals(2, row(stats, "Bruno Petkovic").goals());
            assertEquals(0, row(stats, "Dario Melnjak").goals());
        }

        @Test
        @DisplayName("the related player of a goal gets an assist; the player coming on in a substitution does not")
        void assistFromGoalNotFromSubstitution() {
            List<Player> squad = new ArrayList<>();
            for (int n = 1; n <= 12; n++) {
                squad.add(dinamo.addPlayer("D" + n, n, null));
            }
            Fixture f = new Fixture(round, dinamo, hajduk, Instant.now());
            f.submitLineup(dinamo, squad.subList(0, 11), squad.subList(11, 12), null);
            f.kickOff();
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 20, squad.get(8), squad.get(9), null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.SUBSTITUTION, 70, squad.get(7), squad.get(11), null));
            f.finish();

            List<PlayerStatsEntry> stats = PlayerStatsCalculator.calculate(List.of(f));

            assertEquals(1, row(stats, "D10").assists());
            assertEquals(0, row(stats, "D12").assists());
        }

        @Test
        @DisplayName("yellow and red cards are counted per player")
        void cardsCounted() {
            Player p = dinamo.addPlayer("Josip Misic", 27, null);
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.YELLOW_CARD, 30, p, null, null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.RED_CARD, 80, p, null, null));
            f.finish();

            PlayerStatsEntry r = row(PlayerStatsCalculator.calculate(List.of(f)), "Josip Misic");

            assertEquals(1, r.yellowCards());
            assertEquals(1, r.redCards());
            assertEquals(27, r.shirtNumber());
        }
    }

    @Nested
    @DisplayName("Legacy free-text names")
    class LegacyNames {

        @Test
        @DisplayName("names of one team are merged trimmed and case-insensitively; the same name in another team is another row")
        void namesMergedPerTeam() {
            Fixture first = live(dinamo, hajduk);
            first.addMatchEvent(new MatchEvent(first, hajduk, MatchEventType.GOAL, 10, "Marko Livaja"));
            first.finish();
            Fixture second = live(hajduk, dinamo);
            second.addMatchEvent(new MatchEvent(second, hajduk, MatchEventType.GOAL, 10, " marko livaja "));
            second.addMatchEvent(new MatchEvent(second, dinamo, MatchEventType.GOAL, 20, "Marko Livaja"));
            second.finish();

            List<PlayerStatsEntry> stats = PlayerStatsCalculator.calculate(List.of(first, second));

            List<PlayerStatsEntry> livajas = stats.stream()
                    .filter(e -> e.playerName().equalsIgnoreCase("marko livaja")).toList();
            assertEquals(2, livajas.size());
            PlayerStatsEntry hajdukRow = livajas.stream().filter(e -> e.teamId().equals(hajduk.getId())).findFirst().orElseThrow();
            assertEquals(2, hajdukRow.goals());
            assertEquals(2, hajdukRow.appearances());
            assertEquals("Marko Livaja", hajdukRow.playerName());
            assertNull(hajdukRow.playerId());
            assertNull(hajdukRow.shirtNumber());
        }

        @Test
        @DisplayName("a free-text assist name counts as an assist")
        void textAssistCounted() {
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 23, "Bruno Petkovic", "Martin Baturina", null));
            f.finish();

            assertEquals(1, row(PlayerStatsCalculator.calculate(List.of(f)), "Martin Baturina").assists());
        }

        @Test
        @DisplayName("a free-text name that matches a squad player of the team is credited to that player")
        void textNameMergedIntoSquadPlayer() {
            Player petkovic = dinamo.addPlayer("Bruno Petkovic", 9, "Forward");
            Fixture legacy = live(dinamo, hajduk);
            legacy.addMatchEvent(new MatchEvent(legacy, dinamo, MatchEventType.GOAL, 23, " bruno petkovic", null, null));
            legacy.finish();
            Fixture linked = live(dinamo, hajduk);
            linked.addMatchEvent(new MatchEvent(linked, dinamo, MatchEventType.GOAL, 10, petkovic, null, null));
            linked.finish();

            List<PlayerStatsEntry> stats = PlayerStatsCalculator.calculate(List.of(legacy, linked));

            assertEquals(1, stats.size());
            assertEquals(petkovic.getId(), stats.get(0).playerId());
            assertEquals(9, stats.get(0).shirtNumber());
            assertEquals(2, stats.get(0).goals());
            assertEquals(2, stats.get(0).appearances());
        }

        @Test
        @DisplayName("the same name in the other team is not merged into this team's squad player")
        void textNameOfOtherTeamNotMerged() {
            dinamo.addPlayer("Marko Livaja", 21, null);
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, hajduk, MatchEventType.GOAL, 10, "Marko Livaja"));
            f.finish();

            PlayerStatsEntry r = row(PlayerStatsCalculator.calculate(List.of(f)), "Marko Livaja");

            assertEquals(hajduk.getId(), r.teamId());
            assertNull(r.playerId());
        }
    }

    @Nested
    @DisplayName("Leaderboards")
    class Leaderboards {

        private PlayerStatsEntry entry(String name, int goals, int assists, int yellow, int red) {
            return new PlayerStatsEntry(null, name, null, null, "Team", 1, 90, goals, assists, yellow, red);
        }

        @Test
        @DisplayName("leaders keeps positive values only, best first, ties by name, limited")
        void leadersOrderFilterLimit() {
            List<PlayerStatsEntry> all = List.of(
                    entry("Zed", 2, 0, 0, 0), entry("Amy", 2, 0, 0, 0), entry("Bob", 5, 0, 0, 0),
                    entry("Cid", 1, 0, 0, 0), entry("Dan", 0, 3, 0, 0));

            List<PlayerStatsEntry> top = PlayerStatsCalculator.leaders(all, PlayerStatsEntry::goals,
                    java.util.Comparator.comparingInt(PlayerStatsEntry::goals).reversed(), 3);

            assertEquals(List.of("Bob", "Amy", "Zed"), top.stream().map(PlayerStatsEntry::playerName).toList());
        }

        @Test
        @DisplayName("the stats response builds every board: scorers, assists, G+A, minutes and discipline")
        void responseBoards() {
            List<PlayerStatsEntry> all = List.of(
                    entry("Striker", 4, 0, 0, 0), entry("Playmaker", 1, 5, 2, 0), entry("Hardman", 0, 0, 1, 1));

            var r = com.league.sportsleague.application.dto.PlayerStatsResponse.of(
                    java.util.UUID.randomUUID(), "S", Instant.now(), true, all);

            assertEquals("Striker", r.topScorers().get(0).playerName());
            assertEquals(List.of("Playmaker"), r.topAssists().stream().map(PlayerStatsEntry::playerName).toList());
            assertEquals("Playmaker", r.topContributions().get(0).playerName()); // 6 > 4
            assertEquals(3, r.mostPlayed().size());
            assertEquals(List.of("Hardman", "Playmaker"), r.discipline().stream().map(PlayerStatsEntry::playerName).toList());
        }
    }

    @Nested
    @DisplayName("Appearances, minutes and filtering")
    class AppearancesAndMinutes {

        @Test
        @DisplayName("appearances and minutes come from the lineup and its substitutions")
        void minutesFromLineup() {
            List<Player> squad = new ArrayList<>();
            for (int n = 1; n <= 13; n++) {
                squad.add(dinamo.addPlayer("D" + n, n, null));
            }
            Fixture f = new Fixture(round, dinamo, hajduk, Instant.now());
            f.submitLineup(dinamo, squad.subList(0, 11), squad.subList(11, 13), null);
            f.kickOff();
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.SUBSTITUTION, 60, squad.get(8), squad.get(11), null));
            f.finish();

            List<PlayerStatsEntry> stats = PlayerStatsCalculator.calculate(List.of(f));

            assertEquals(60, row(stats, "D9").minutesPlayed());
            assertEquals(30, row(stats, "D12").minutesPlayed());
            assertEquals(90, row(stats, "D1").minutesPlayed());
            assertEquals(1, row(stats, "D12").appearances());
            assertTrue(stats.stream().noneMatch(e -> e.playerName().equals("D13")), "unused substitute has no row");
            assertEquals(12, stats.stream().filter(e -> e.teamId().equals(dinamo.getId())).count());
        }

        @Test
        @DisplayName("volleyball appearances are counted but minutes stay 0")
        void volleyballMinutesStayZero() {
            Team home = new Team("Mladost", "MLA", "VOLLEYBALL", "Hall", "Zagreb", 1945);
            Team away = new Team("Split", "SPL", "VOLLEYBALL", "Hall", "Split", 1945);
            List<Player> squad = new ArrayList<>();
            for (int n = 1; n <= 6; n++) {
                squad.add(home.addPlayer("V" + n, n, null));
            }
            Fixture f = new Fixture(round, home, away, Instant.now());
            f.submitLineup(home, squad, List.of(), null);
            f.kickOff();
            f.finish();

            PlayerStatsEntry r = row(PlayerStatsCalculator.calculate(List.of(f)), "V1");

            assertEquals(1, r.appearances());
            assertEquals(0, r.minutesPlayed());
        }

        @Test
        @DisplayName("only FINISHED fixtures count")
        void inProgressIgnored() {
            Player striker = dinamo.addPlayer("Bruno Petkovic", 9, null);
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 10, striker, null, null));

            assertTrue(PlayerStatsCalculator.calculate(List.of(f)).isEmpty());
        }

        @Test
        @DisplayName("rows are ordered by goals, then assists, then name")
        void ordering() {
            Fixture f = live(dinamo, hajduk);
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 10, "Zed", "Alpha", null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 20, "Zed", null, null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.GOAL, 30, "Beta", null, null));
            f.addMatchEvent(new MatchEvent(f, dinamo, MatchEventType.YELLOW_CARD, 40, "Charlie", null, null));
            f.finish();

            List<String> order = PlayerStatsCalculator.calculate(List.of(f)).stream()
                    .map(PlayerStatsEntry::playerName).toList();

            assertEquals(List.of("Zed", "Beta", "Alpha", "Charlie"), order);
        }
    }
}
