package com.league.sportsleague.domain.ai;

/** Lightweight summary of a single match event for chronicle generation. */
public record MatchEventSummary(
        int minute,
        String eventType,
        String teamName,
        String playerName
) {}
