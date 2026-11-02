package com.league.sportsleague.domain.ai;

import java.util.List;

/**
 * Input to the AI chronicle port.
 * Carries structured fixture data; the adapter converts this into a prompt.
 */
public record ChronicleRequest(
        String homeTeam,
        String awayTeam,
        int homeScore,
        int awayScore,
        List<MatchEventSummary> events,
        Language language
) {}
