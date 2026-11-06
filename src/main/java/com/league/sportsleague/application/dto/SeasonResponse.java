package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Season;

import java.util.UUID;

public record SeasonResponse(
        UUID id,
        UUID leagueId,
        String leagueName,
        String leagueSlug,
        String name,
        int yearStart,
        int yearEnd,
        String status,
        boolean hasSchedule,
        int roundCount
) {
    public static SeasonResponse from(Season season) {
        int rounds = season.getRounds().size();
        return new SeasonResponse(
                season.getId(),
                season.getLeague().getId(),
                season.getLeague().getName(),
                season.getLeague().getSlug().value(),
                season.getName(),
                season.getYearStart(),
                season.getYearEnd(),
                season.getStatus().name(),
                rounds > 0,
                rounds
        );
    }
}
