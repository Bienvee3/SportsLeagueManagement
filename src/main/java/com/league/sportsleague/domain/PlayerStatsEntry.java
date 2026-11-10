package com.league.sportsleague.domain;

import java.util.UUID;

/**
 * One row of the season player statistics. Not a JPA entity: computed on demand from finished fixtures.
 * {@code playerId} and {@code shirtNumber} are null for a player known only by the free-text name of
 * legacy events.
 */
public record PlayerStatsEntry(
        UUID playerId,
        String playerName,
        Integer shirtNumber,
        UUID teamId,
        String teamName,
        int appearances,
        int minutesPlayed,
        int goals,
        int assists,
        int yellowCards,
        int redCards
) {}
