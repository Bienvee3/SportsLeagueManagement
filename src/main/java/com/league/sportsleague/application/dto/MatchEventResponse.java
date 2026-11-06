package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.MatchEvent;

/**
 * Read-only projection of a MatchEvent for display in fixture detail views.
 */
public record MatchEventResponse(
        int minute,
        String eventType,
        String teamName,
        String teamCode,
        String playerName,
        String assistingPlayer
) {
    public static MatchEventResponse from(MatchEvent e) {
        return new MatchEventResponse(
                e.getMatchMinute().value(),
                e.getEventType().name(),
                e.getTeam().getName(),
                e.getTeam().getShortCode().value(),
                e.getPlayerName(),
                e.getAssistingPlayer()
        );
    }
}
