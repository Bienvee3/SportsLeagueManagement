package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.model.enums.MatchEventType;
import com.league.sportsleague.domain.model.enums.SuspensionReason;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Who of a team may not play a fixture. Suspensions are derived from the season's finished matches,
 * never stored: a red card, or reaching every {@value #YELLOW_CARD_LIMIT}th yellow card of the season,
 * bans the player from the team's next match. Only events linked to squad players count.
 * Pure domain logic: no JPA, no Spring.
 */
public final class SuspensionCalculator {

    public static final int YELLOW_CARD_LIMIT = 5;

    private SuspensionCalculator() {
    }

    /**
     * The suspended players of {@code team} for {@code target}, by player id.
     *
     * <p>The ban is served in the team's next played match: only the last FINISHED fixture of the team
     * scheduled before {@code target} can trigger it. Postponed and cancelled fixtures are never played,
     * so they do not consume a ban.
     *
     * @param seasonFixtures the fixtures of the season of {@code target} (any status)
     */
    public static Map<UUID, SuspensionReason> suspendedFor(Fixture target, Team team, List<Fixture> seasonFixtures) {
        List<Fixture> played = seasonFixtures.stream()
                .filter(f -> f.getStatus() == FixtureStatus.FINISHED)
                .filter(f -> !f.getId().equals(target.getId()))
                .filter(f -> involves(f, team))
                .filter(f -> f.getScheduledTime().isBefore(target.getScheduledTime()))
                .sorted(Comparator.comparing(Fixture::getScheduledTime))
                .toList();
        if (played.isEmpty()) {
            return Map.of();
        }
        Fixture last = played.get(played.size() - 1);

        Map<UUID, SuspensionReason> suspended = new LinkedHashMap<>();
        Map<UUID, Integer> yellowsInLast = new HashMap<>();
        for (MatchEvent event : playerEventsOf(last, team)) {
            UUID playerId = event.getPlayer().getId();
            if (event.getEventType() == MatchEventType.RED_CARD) {
                suspended.put(playerId, SuspensionReason.RED_CARD);
            } else if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                yellowsInLast.merge(playerId, 1, Integer::sum);
            }
        }
        if (yellowsInLast.isEmpty()) {
            return suspended;
        }
        Map<UUID, Integer> yellowsBefore = new HashMap<>();
        for (Fixture earlier : played.subList(0, played.size() - 1)) {
            for (MatchEvent event : playerEventsOf(earlier, team)) {
                if (event.getEventType() == MatchEventType.YELLOW_CARD) {
                    yellowsBefore.merge(event.getPlayer().getId(), 1, Integer::sum);
                }
            }
        }
        yellowsInLast.forEach((playerId, inLast) -> {
            int before = yellowsBefore.getOrDefault(playerId, 0);
            int after = before + inLast;
            if (after / YELLOW_CARD_LIMIT > before / YELLOW_CARD_LIMIT) {
                suspended.putIfAbsent(playerId, SuspensionReason.YELLOW_CARDS);
            }
        });
        return suspended;
    }

    private static boolean involves(Fixture fixture, Team team) {
        return fixture.getHomeTeam().getId().equals(team.getId())
                || fixture.getAwayTeam().getId().equals(team.getId());
    }

    private static List<MatchEvent> playerEventsOf(Fixture fixture, Team team) {
        return fixture.getEvents().stream()
                .filter(e -> e.getPlayer() != null)
                .filter(e -> e.getTeam().getId().equals(team.getId()))
                .toList();
    }
}
