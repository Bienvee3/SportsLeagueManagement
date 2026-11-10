package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.PlayerStatsEntry;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Season player statistics. {@code minutesTracked} is false for sports without a game clock
 * (volleyball), where the minutes column is meaningless.
 */
public record PlayerStatsResponse(
        UUID seasonId,
        String seasonName,
        Instant computedAt,
        boolean minutesTracked,
        List<PlayerStatsEntry> entries
) {}
