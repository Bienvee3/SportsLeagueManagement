package com.league.sportsleague.domain;

import com.league.sportsleague.domain.model.Fixture;
import com.league.sportsleague.domain.model.LineupEntry;
import com.league.sportsleague.domain.model.MatchEvent;
import com.league.sportsleague.domain.model.Player;
import com.league.sportsleague.domain.model.Team;
import com.league.sportsleague.domain.model.enums.FixtureStatus;
import com.league.sportsleague.domain.model.enums.MatchEventType;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure domain service: season statistics per player from a list of fixtures. Only FINISHED fixtures
 * count, as in the standings.
 *
 * <p>Players linked to the squad are counted by id. Legacy events that only carry a free-text name are
 * credited to the squad player of that team with the same name (trimmed, case-insensitive); names that
 * match no squad player are counted per team and name, so history recorded before squads existed stays
 * visible. Appearances and minutes come from the lineup when the team filed one; without a
 * lineup, every player named in an event of the team counts as one appearance and minutes are unknown.
 */
public final class PlayerStatsCalculator {

    private PlayerStatsCalculator() {
    }

    public static List<PlayerStatsEntry> calculate(List<Fixture> fixtures) {
        Map<String, Row> rows = new LinkedHashMap<>();
        for (Fixture fixture : fixtures) {
            if (fixture.getStatus() != FixtureStatus.FINISHED) {
                continue;
            }
            LineupRules rules = fixture.getLineupRules();
            int lastEventMinute = fixture.getEvents().stream()
                    .mapToInt(e -> e.getMatchMinute().value())
                    .max().orElse(0);
            int endMinute = Math.max(rules.regulationMinutes(), lastEventMinute);
            for (Team team : List.of(fixture.getHomeTeam(), fixture.getAwayTeam())) {
                List<MatchEvent> teamEvents = fixture.getEvents().stream()
                        .filter(e -> e.getTeam().getId().equals(team.getId()))
                        .toList();
                countAppearances(fixture, team, teamEvents, rules, endMinute, rows);
                countEvents(team, teamEvents, rows);
            }
        }
        return rows.values().stream()
                .map(Row::toEntry)
                .sorted(Comparator.comparingInt(PlayerStatsEntry::goals).reversed()
                        .thenComparing(Comparator.comparingInt(PlayerStatsEntry::assists).reversed())
                        .thenComparing(PlayerStatsEntry::playerName))
                .toList();
    }

    private static void countAppearances(Fixture fixture, Team team, List<MatchEvent> teamEvents,
                                         LineupRules rules, int endMinute, Map<String, Row> rows) {
        List<LineupEntry> lineup = fixture.getLineup(team);
        if (!lineup.isEmpty()) {
            PitchTimeline timeline = fixture.replayPitch(team);
            for (LineupEntry entry : lineup) {
                Player player = entry.getPlayer();
                if (timeline.appearedPlayerIds().contains(player.getId())) {
                    Row row = row(rows, team, player);
                    row.appearances++;
                    if (rules.regulationMinutes() > 0) {
                        row.minutesPlayed += timeline.minutesPlayed(player.getId(), endMinute);
                    }
                }
            }
            return;
        }
        Set<Row> named = new LinkedHashSet<>();
        for (MatchEvent event : teamEvents) {
            Row row = mainRow(rows, team, event);
            if (row != null) {
                named.add(row);
            }
        }
        named.forEach(row -> row.appearances++);
    }

    private static void countEvents(Team team, List<MatchEvent> teamEvents, Map<String, Row> rows) {
        for (MatchEvent event : teamEvents) {
            Row main = mainRow(rows, team, event);
            if (main == null) {
                continue;
            }
            switch (event.getEventType()) {
                case GOAL, PENALTY -> {
                    main.goals++;
                    Row assist = assistRow(rows, team, event);
                    if (assist != null) {
                        assist.assists++;
                    }
                }
                case YELLOW_CARD -> main.yellowCards++;
                case RED_CARD -> main.redCards++;
                case OWN_GOAL, SUBSTITUTION -> { }
            }
        }
    }

    /** The player the event is about: the squad player, or the free-text name of a legacy event. */
    private static Row mainRow(Map<String, Row> rows, Team team, MatchEvent event) {
        if (event.getPlayer() != null) {
            return row(rows, team, event.getPlayer());
        }
        return textRow(rows, team, event.getPlayerName());
    }

    /** The assist provider of a goal: the related squad player, or the free-text assist name. */
    private static Row assistRow(Map<String, Row> rows, Team team, MatchEvent event) {
        if (event.getRelatedPlayer() != null) {
            return row(rows, team, event.getRelatedPlayer());
        }
        if (event.getPlayer() == null) {
            return textRow(rows, team, event.getAssistingPlayer());
        }
        return null;
    }

    private static Row row(Map<String, Row> rows, Team team, Player player) {
        return rows.computeIfAbsent(player.getId().toString(),
                key -> new Row(player.getId(), player.getName(), player.getShirtNumber(), team));
    }

    private static Row textRow(Map<String, Row> rows, Team team, String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String trimmed = name.trim();
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        for (Player player : team.getPlayers()) {
            if (player.getName().trim().toLowerCase(Locale.ROOT).equals(normalized)) {
                return row(rows, team, player);
            }
        }
        return rows.computeIfAbsent(team.getId() + "|" + normalized,
                key -> new Row(null, trimmed, null, team));
    }

    /**
     * A leaderboard: the entries with a positive {@code metric}, best first ({@code order}), at most
     * {@code limit} of them.
     */
    public static List<PlayerStatsEntry> leaders(List<PlayerStatsEntry> entries,
                                                 java.util.function.ToIntFunction<PlayerStatsEntry> metric,
                                                 Comparator<PlayerStatsEntry> order,
                                                 int limit) {
        return entries.stream()
                .filter(e -> metric.applyAsInt(e) > 0)
                .sorted(order.thenComparing(PlayerStatsEntry::playerName))
                .limit(limit)
                .toList();
    }

    private static final class Row {
        private final UUID playerId;
        private final String playerName;
        private final Integer shirtNumber;
        private final UUID teamId;
        private final String teamName;
        private int appearances;
        private int minutesPlayed;
        private int goals;
        private int assists;
        private int yellowCards;
        private int redCards;

        Row(UUID playerId, String playerName, Integer shirtNumber, Team team) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.shirtNumber = shirtNumber;
            this.teamId = team.getId();
            this.teamName = team.getName();
        }

        PlayerStatsEntry toEntry() {
            return new PlayerStatsEntry(playerId, playerName, shirtNumber, teamId, teamName,
                    appearances, minutesPlayed, goals, assists, yellowCards, redCards);
        }
    }
}
