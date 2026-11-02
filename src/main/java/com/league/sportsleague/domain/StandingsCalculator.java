package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.enums.FixtureStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure domain service: computes the standings table from a list of fixtures.
 * No Spring beans, no JPA — fully stateless and unit-testable.
 *
 * <p>Tie-break cascade (5 levels):
 * <ol>
 *   <li>Total points (W=3, D=1, L=0)</li>
 *   <li>Overall goal difference</li>
 *   <li>Overall goals scored</li>
 *   <li>Head-to-head points among tied teams</li>
 *   <li>Head-to-head goal difference among tied teams</li>
 * </ol>
 */
public final class StandingsCalculator {

    private StandingsCalculator() {}

    /**
     * Calculates the standings from the given fixture list.
     * Only {@link FixtureStatus#FINISHED} fixtures contribute to statistics.
     * All teams present in any fixture (regardless of status) appear in the result.
     *
     * @param fixtures all fixtures for the season (may include non-finished ones)
     * @return sorted standings list, index 0 = first place
     */
    public static List<StandingsEntry> calculate(List<Fixture> fixtures) {
        if (fixtures.isEmpty()) {
            return List.of();
        }

        // Build initial blank entries for every team encountered
        Map<UUID, StandingsEntry> table = new HashMap<>();

        for (Fixture f : fixtures) {
            UUID homeId = f.getHomeTeam().getId();
            UUID awayId = f.getAwayTeam().getId();
            String homeName = f.getHomeTeam().getName();
            String awayName = f.getAwayTeam().getName();

            table.putIfAbsent(homeId, StandingsEntry.blank(homeId, homeName));
            table.putIfAbsent(awayId, StandingsEntry.blank(awayId, awayName));

            if (f.getStatus() != FixtureStatus.FINISHED) {
                continue;
            }

            int hg = f.getHomeScore();
            int ag = f.getAwayScore();

            if (hg > ag) {
                table.put(homeId, table.get(homeId).withHomeWin(hg, ag));
                table.put(awayId, table.get(awayId).withAwayLoss(ag, hg));
            } else if (hg < ag) {
                table.put(homeId, table.get(homeId).withHomeLoss(hg, ag));
                table.put(awayId, table.get(awayId).withAwayWin(ag, hg));
            } else {
                table.put(homeId, table.get(homeId).withHomeDraw(hg, ag));
                table.put(awayId, table.get(awayId).withAwayDraw(ag, hg));
            }
        }

        List<StandingsEntry> sorted = new ArrayList<>(table.values());
        sorted.sort(standingsComparator(fixtures));
        return List.copyOf(sorted);
    }

    // ── Comparator ────────────────────────────────────────────────────────────

    private static Comparator<StandingsEntry> standingsComparator(List<Fixture> allFixtures) {
        return Comparator
                .<StandingsEntry, Integer>comparing(e -> -e.points())
                .thenComparingInt(e -> -e.goalDifference())
                .thenComparingInt(e -> -e.goalsFor())
                .thenComparing((a, b) -> compareHeadToHead(a, b, allFixtures, true))
                .thenComparing((a, b) -> compareHeadToHead(a, b, allFixtures, false));
    }

    /**
     * Head-to-head comparison between two teams.
     *
     * @param byPoints {@code true} → compare H2H points; {@code false} → compare H2H goal difference
     */
    private static int compareHeadToHead(StandingsEntry a, StandingsEntry b,
                                         List<Fixture> fixtures, boolean byPoints) {
        int aPoints = 0, aGD = 0;
        int bPoints = 0, bGD = 0;

        for (Fixture f : fixtures) {
            if (f.getStatus() != FixtureStatus.FINISHED) continue;

            UUID homeId = f.getHomeTeam().getId();
            UUID awayId = f.getAwayTeam().getId();

            boolean isH2H = (homeId.equals(a.teamId()) && awayId.equals(b.teamId()))
                         || (homeId.equals(b.teamId()) && awayId.equals(a.teamId()));
            if (!isH2H) continue;

            int hg = f.getHomeScore();
            int ag = f.getAwayScore();

            if (homeId.equals(a.teamId())) {
                // a is home
                int diff = hg - ag;
                aGD += diff;
                bGD -= diff;
                if (hg > ag) aPoints += 3;
                else if (hg == ag) { aPoints += 1; bPoints += 1; }
                else bPoints += 3;
            } else {
                // b is home
                int diff = hg - ag;
                bGD += diff;
                aGD -= diff;
                if (hg > ag) bPoints += 3;
                else if (hg == ag) { aPoints += 1; bPoints += 1; }
                else aPoints += 3;
            }
        }

        if (byPoints) {
            return Integer.compare(bPoints, aPoints); // higher points first
        } else {
            return Integer.compare(bGD, aGD); // higher GD first
        }
    }
}
