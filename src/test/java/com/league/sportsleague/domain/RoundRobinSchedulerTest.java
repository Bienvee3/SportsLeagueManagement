package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for RoundRobinScheduler.
 * No Spring context, no database — just algorithm verification.
 */
class RoundRobinSchedulerTest {

    private League league;
    private Season season;
    private Instant baseTime;

    @BeforeEach
    void setUp() {
        league   = new League("HNL", "hnl", "FOOTBALL", "Croatia");
        season   = new Season(league, "2026/2027", 2026, 2027, SeasonStatus.UPCOMING);
        baseTime = Instant.parse("2026-08-01T18:00:00Z");
    }

    private List<Team> makeTeams(int n) {
        List<Team> teams = new ArrayList<>();
        String[] codes = {"AAA","BBB","CCC","DDD","EEE","FFF","GGG","HHH","III","JJJ"};
        for (int i = 0; i < n; i++) {
            teams.add(new Team("Team " + codes[i], codes[i], "Stadium", "City", 2000));
        }
        return teams;
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("0 teams → 0 rounds")
    void zeroTeamsProducesNoRounds() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, List.of(), baseTime);
        assertTrue(rounds.isEmpty());
    }

    @Test
    @DisplayName("1 team → 0 rounds (cannot form a match)")
    void oneTeamProducesNoRounds() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(1), baseTime);
        assertTrue(rounds.isEmpty());
    }

    // ── Fixture counts ────────────────────────────────────────────────────────

    @Test
    @DisplayName("2 teams → 1 round, 1 fixture")
    void twoTeams() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(2), baseTime);
        assertEquals(1, rounds.size());
        assertEquals(1, rounds.get(0).getFixtures().size());
    }

    @Test
    @DisplayName("3 teams → 3 rounds, 1 fixture each (10 total fixtures counting byes=0)")
    void threeTeams() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(3), baseTime);
        assertEquals(3, rounds.size());
        rounds.forEach(r -> assertEquals(1, r.getFixtures().size()));
    }

    @Test
    @DisplayName("4 teams → 3 rounds, 2 fixtures each, 6 total")
    void fourTeams() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        assertEquals(3, rounds.size());
        rounds.forEach(r -> assertEquals(2, r.getFixtures().size()));
        long total = rounds.stream().mapToLong(r -> r.getFixtures().size()).sum();
        assertEquals(6, total);
    }

    @Test
    @DisplayName("5 teams → 5 rounds, 2 fixtures each, 10 total")
    void fiveTeams() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(5), baseTime);
        assertEquals(5, rounds.size());
        rounds.forEach(r -> assertEquals(2, r.getFixtures().size()));
        long total = rounds.stream().mapToLong(r -> r.getFixtures().size()).sum();
        assertEquals(10, total);
    }

    @Test
    @DisplayName("6 teams → 5 rounds, 3 fixtures each, 15 total")
    void sixTeams() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(6), baseTime);
        assertEquals(5, rounds.size());
        rounds.forEach(r -> assertEquals(3, r.getFixtures().size()));
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
    @DisplayName("N teams → N*(N-1)/2 total fixtures (single round-robin formula)")
    void totalFixturesMatchesFormula(int n) {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(n), baseTime);
        long total = rounds.stream().mapToLong(r -> r.getFixtures().size()).sum();
        assertEquals((long) n * (n - 1) / 2, total, "Total fixtures must be N*(N-1)/2 for N=" + n);
    }

    // ── No team plays twice in the same round ─────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
    @DisplayName("No team appears twice in the same round")
    void noTeamPlaysTwiceInSameRound(int n) {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(n), baseTime);
        for (Round round : rounds) {
            Set<UUID> teamIds = new HashSet<>();
            for (Fixture f : round.getFixtures()) {
                assertTrue(teamIds.add(f.getHomeTeam().getId()),
                        "Home team appeared twice in round " + round.getRoundNumber());
                assertTrue(teamIds.add(f.getAwayTeam().getId()),
                        "Away team appeared twice in round " + round.getRoundNumber());
            }
        }
    }

    // ── Every pair plays exactly once ─────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
    @DisplayName("Every pair of teams plays exactly once")
    void everyPairPlaysExactlyOnce(int n) {
        List<Team> teams = makeTeams(n);
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, teams, baseTime);

        // Build a map of pair → count
        Map<String, Integer> pairCount = new HashMap<>();
        for (Round round : rounds) {
            for (Fixture f : round.getFixtures()) {
                String pair = pairKey(f.getHomeTeam().getId(), f.getAwayTeam().getId());
                pairCount.merge(pair, 1, Integer::sum);
            }
        }

        // Every pair among the n teams must appear exactly once
        for (int i = 0; i < teams.size(); i++) {
            for (int j = i + 1; j < teams.size(); j++) {
                String pair = pairKey(teams.get(i).getId(), teams.get(j).getId());
                assertEquals(1, pairCount.getOrDefault(pair, 0),
                        "Pair " + pair + " should appear exactly once");
            }
        }
    }

    private String pairKey(UUID a, UUID b) {
        // Order-independent key
        return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
    }

    // ── No fixture has homeTeam == awayTeam ───────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8})
    @DisplayName("No fixture has the same team as both home and away")
    void noFixtureHasSameHomeAndAway(int n) {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(n), baseTime);
        for (Round round : rounds) {
            for (Fixture f : round.getFixtures()) {
                assertNotEquals(f.getHomeTeam().getId(), f.getAwayTeam().getId(),
                        "Fixture must not have the same team as home and away");
            }
        }
    }

    // ── Home/away balance ─────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8})
    @DisplayName("Each team's home and away game counts differ by at most 1")
    void homeAwayBalanceDiffersByAtMostOne(int n) {
        List<Team> teams = makeTeams(n);
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, teams, baseTime);

        Map<UUID, Integer> homeCount = new HashMap<>();
        Map<UUID, Integer> awayCount = new HashMap<>();
        for (Round round : rounds) {
            for (Fixture f : round.getFixtures()) {
                homeCount.merge(f.getHomeTeam().getId(), 1, Integer::sum);
                awayCount.merge(f.getAwayTeam().getId(), 1, Integer::sum);
            }
        }

        for (Team team : teams) {
            int home = homeCount.getOrDefault(team.getId(), 0);
            int away = awayCount.getOrDefault(team.getId(), 0);
            assertTrue(Math.abs(home - away) <= 1,
                    "Team " + team.getShortCode().value() +
                    " home=" + home + " away=" + away + " diff=" + Math.abs(home - away));
        }
    }

    // ── Round metadata ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Rounds are numbered sequentially starting from 1")
    void roundNumbersAreSequential() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        for (int i = 0; i < rounds.size(); i++) {
            assertEquals(i + 1, rounds.get(i).getRoundNumber());
        }
    }

    @Test
    @DisplayName("Each round has a non-blank name")
    void roundsHaveNames() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        rounds.forEach(r -> assertNotNull(r.getName()));
        rounds.forEach(r -> assertFalse(r.getName().isBlank()));
    }

    @Test
    @DisplayName("Fixtures in round N are scheduled 7*(N-1) days after baseTime")
    void fixtureTimesAreStaggeredByWeek() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        for (int i = 0; i < rounds.size(); i++) {
            Instant expected = baseTime.plusSeconds(7L * 24 * 3600 * i);
            for (Fixture f : rounds.get(i).getFixtures()) {
                assertEquals(expected, f.getScheduledTime(),
                        "Round " + (i + 1) + " fixtures must be at baseTime + " + i + " weeks");
            }
        }
    }

    @Test
    @DisplayName("All rounds have SCHEDULED status initially")
    void roundsInitiallyScheduled() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        rounds.forEach(r -> assertEquals(RoundStatus.SCHEDULED, r.getStatus()));
    }

    @Test
    @DisplayName("All fixtures have SCHEDULED status initially")
    void fixturesInitiallyScheduled() {
        List<Round> rounds = RoundRobinScheduler.generateSchedule(season, makeTeams(4), baseTime);
        rounds.stream()
              .flatMap(r -> r.getFixtures().stream())
              .forEach(f -> assertEquals(FixtureStatus.SCHEDULED, f.getStatus()));
    }
}
