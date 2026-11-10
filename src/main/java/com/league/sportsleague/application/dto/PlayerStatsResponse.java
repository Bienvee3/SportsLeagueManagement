package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.PlayerStatsCalculator;
import com.league.sportsleague.domain.PlayerStatsEntry;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Season player statistics: the full table plus the leaderboards shown side by side on the stats page.
 * {@code minutesTracked} is false for sports without a game clock (volleyball); "most played" then
 * ranks by appearances instead of minutes.
 *
 * @param topScorers       most goals
 * @param topAssists       most assists
 * @param topContributions most goals + assists
 * @param mostPlayed       most minutes (or appearances when minutes are not tracked)
 * @param discipline       most red cards, then most yellow cards
 */
public record PlayerStatsResponse(
        UUID seasonId,
        String seasonName,
        Instant computedAt,
        boolean minutesTracked,
        List<PlayerStatsEntry> entries,
        List<PlayerStatsEntry> topScorers,
        List<PlayerStatsEntry> topAssists,
        List<PlayerStatsEntry> topContributions,
        List<PlayerStatsEntry> mostPlayed,
        List<PlayerStatsEntry> discipline
) {
    /** Size of each leaderboard. */
    public static final int LEADERBOARD_SIZE = 5;

    public static PlayerStatsResponse of(UUID seasonId, String seasonName, Instant computedAt,
                                         boolean minutesTracked, List<PlayerStatsEntry> entries) {
        int n = LEADERBOARD_SIZE;
        return new PlayerStatsResponse(seasonId, seasonName, computedAt, minutesTracked, entries,
                PlayerStatsCalculator.leaders(entries, PlayerStatsEntry::goals,
                        Comparator.comparingInt(PlayerStatsEntry::goals).reversed()
                                .thenComparing(Comparator.comparingInt(PlayerStatsEntry::assists).reversed()), n),
                PlayerStatsCalculator.leaders(entries, PlayerStatsEntry::assists,
                        Comparator.comparingInt(PlayerStatsEntry::assists).reversed()
                                .thenComparing(Comparator.comparingInt(PlayerStatsEntry::goals).reversed()), n),
                PlayerStatsCalculator.leaders(entries, e -> e.goals() + e.assists(),
                        Comparator.<PlayerStatsEntry>comparingInt(e -> -(e.goals() + e.assists()))
                                .thenComparing(Comparator.comparingInt(PlayerStatsEntry::goals).reversed()), n),
                minutesTracked
                        ? PlayerStatsCalculator.leaders(entries, PlayerStatsEntry::minutesPlayed,
                                Comparator.comparingInt(PlayerStatsEntry::minutesPlayed).reversed(), n)
                        : PlayerStatsCalculator.leaders(entries, PlayerStatsEntry::appearances,
                                Comparator.comparingInt(PlayerStatsEntry::appearances).reversed(), n),
                PlayerStatsCalculator.leaders(entries, e -> e.redCards() + e.yellowCards(),
                        Comparator.comparingInt(PlayerStatsEntry::redCards).reversed()
                                .thenComparing(Comparator.comparingInt(PlayerStatsEntry::yellowCards).reversed()), n));
    }
}
