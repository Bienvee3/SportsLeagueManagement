package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Season;

import java.util.UUID;

public record SeasonResponse(
        UUID id,
        String name,
        int yearStart,
        int yearEnd,
        String status
) {
    public static SeasonResponse from(Season season) {
        return new SeasonResponse(
                season.getId(),
                season.getName(),
                season.getYearStart(),
                season.getYearEnd(),
                season.getStatus().name()
        );
    }
}
