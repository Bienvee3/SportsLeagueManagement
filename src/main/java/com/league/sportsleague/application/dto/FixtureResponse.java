package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Fixture;

import java.time.Instant;
import java.util.UUID;

public record FixtureResponse(
        UUID id,
        String homeTeamCode,
        String homeTeamName,
        String awayTeamCode,
        String awayTeamName,
        Integer homeScore,
        Integer awayScore,
        String status,
        Instant scheduledTime
) {
    public static FixtureResponse from(Fixture fixture) {
        return new FixtureResponse(
                fixture.getId(),
                fixture.getHomeTeam().getShortCode().value(),
                fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getShortCode().value(),
                fixture.getAwayTeam().getName(),
                fixture.getHomeScore(),
                fixture.getAwayScore(),
                fixture.getStatus().name(),
                fixture.getScheduledTime()
        );
    }
}
