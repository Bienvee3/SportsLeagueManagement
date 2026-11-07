package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.MatchEvent;

import java.util.UUID;

/**
 * Read-only projection of a MatchEvent for display in fixture detail views.
 */
public record MatchEventResponse(
        UUID id,
        int minute,
        String eventType,
        String teamName,
        String teamCode,
        String playerName,
        String assistingPlayer
) {
    public static MatchEventResponse from(MatchEvent e) {
        return new MatchEventResponse(
                e.getId(),
                e.getMatchMinute().value(),
                e.getEventType().name(),
                e.getTeam().getName(),
                e.getTeam().getShortCode().value(),
                e.getPlayerName(),
                e.getAssistingPlayer()
        );
    }
}
