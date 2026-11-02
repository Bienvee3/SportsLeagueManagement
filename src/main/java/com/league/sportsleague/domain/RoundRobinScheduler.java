package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.*;
import com.league.sportsleague.domain.model.enums.RoundStatus;

import java.time.Instant;
import java.util.*;

/**
 * Pure domain service — no Spring annotations, no state, no I/O.
 * Generates a single round-robin schedule using the circle (polygon) rotation method.
 *
 * Algorithm:
 * - For N even: N-1 rounds, N/2 fixtures per round.
 * - For N odd: add a phantom BYE slot (null) to make N+1 (even).
 *   Fixtures involving BYE are skipped, yielding N rounds with (N-1)/2 real fixtures.
 * - Fix slot 0 at each rotation. Rotate slots 1..N-1 clockwise every round.
 *
 * Home/away balance — greedy tracker:
 * For each fixture (teamA, teamB), compare their accumulated balance:
 *   diff = awayGames - homeGames  (positive means "wants home more")
 * If diffA > diffB → teamA is home.
 * If diffB > diffA → teamB is home.
 * If equal         → tiebreak by (roundIndex + matchIndex) % 2:
 *                    0 → teamA (left slot) is home; 1 → teamB (right slot) is home.
 * This guarantees |homeGames − awayGames| ≤ 1 for every team.
 */
public class RoundRobinScheduler {

    private RoundRobinScheduler() {}

    /**
     * Generates a single round-robin schedule.
     *
     * @param season         the season these rounds belong to
     * @param teams          participating teams (need ≥ 2 to produce rounds)
     * @param firstMatchTime scheduled time for round-1 fixtures;
     *                       round N scheduled at firstMatchTime + (N-1) × 7 days
     * @return ordered list of Round objects with Fixtures attached;
     *         caller persists via {@code season.addRound(round)} then saves.
     */
    public static List<Round> generateSchedule(Season season,
                                               List<Team> teams,
                                               Instant firstMatchTime) {
        if (teams.size() < 2) {
            return Collections.emptyList();
        }

        // Mutable circle; null = phantom BYE for odd N
        List<Team> circle = new ArrayList<>(teams);
        if (circle.size() % 2 != 0) {
            circle.add(null);
        }

        int n               = circle.size();    // always even
        int numRounds       = n - 1;
        int matchesPerRound = n / 2;

        // Greedy home/away tracker
        Map<UUID, Integer> homeCount = new HashMap<>();
        Map<UUID, Integer> awayCount = new HashMap<>();

        List<Round> rounds = new ArrayList<>(numRounds);

        for (int roundIdx = 0; roundIdx < numRounds; roundIdx++) {
            int    roundNumber    = roundIdx + 1;
            Instant scheduledTime = firstMatchTime.plusSeconds(7L * 24 * 3600 * roundIdx);

            Round round = new Round(season, roundNumber, "Round " + roundNumber,
                                    null, null, RoundStatus.SCHEDULED);

            for (int matchIdx = 0; matchIdx < matchesPerRound; matchIdx++) {
                Team teamA = circle.get(matchIdx);
                Team teamB = circle.get(n - 1 - matchIdx);

                if (teamA == null || teamB == null) continue; // skip BYE

                // Compare accumulated home/away need
                int diffA = awayCount.getOrDefault(teamA.getId(), 0)
                          - homeCount.getOrDefault(teamA.getId(), 0);
                int diffB = awayCount.getOrDefault(teamB.getId(), 0)
                          - homeCount.getOrDefault(teamB.getId(), 0);

                boolean aIsHome;
                if (diffA > diffB) {
                    aIsHome = true;   // teamA needs home more
                } else if (diffB > diffA) {
                    aIsHome = false;  // teamB needs home more
                } else {
                    // Equal desire — alternate by round+match to prevent systematic bias
                    aIsHome = (roundIdx + matchIdx) % 2 == 0;
                }

                Team homeTeam = aIsHome ? teamA : teamB;
                Team awayTeam = aIsHome ? teamB : teamA;

                homeCount.merge(homeTeam.getId(), 1, Integer::sum);
                awayCount.merge(awayTeam.getId(), 1, Integer::sum);

                round.addFixture(new Fixture(round, homeTeam, awayTeam, scheduledTime));
            }

            rounds.add(round);

            // Rotate slots 1..n-1 clockwise: last element moves to position 1
            // [0, 1, 2, 3, 4, 5] → [0, 5, 1, 2, 3, 4]
            Team last = circle.remove(circle.size() - 1);
            circle.add(1, last);
        }

        return rounds;
    }
}
