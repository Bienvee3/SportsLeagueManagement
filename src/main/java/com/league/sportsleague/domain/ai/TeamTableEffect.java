package com.league.sportsleague.domain.ai;

/**
 * What one finished match did to the league table for one of its teams: position and points before the
 * match was counted and after. {@code positionBefore} is null when the team had no finished match before
 * (it was not in the table yet).
 */
public record TeamTableEffect(
        String teamName,
        Integer positionBefore,
        int positionAfter,
        int pointsBefore,
        int pointsAfter
) {}
