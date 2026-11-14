package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Fixture;

import java.time.Instant;
import java.util.UUID;

/** A SCHEDULED match whose kick-off time has long passed without anyone starting it. */
public record OverdueFixtureResponse(
        UUID fixtureId,
        String homeTeam,
        String awayTeam,
        int roundNumber,
        Instant scheduledTime
) {
    public static OverdueFixtureResponse from(Fixture fixture) {
        return new OverdueFixtureResponse(
                fixture.getId(),
                fixture.getHomeTeam().getName(),
                fixture.getAwayTeam().getName(),
                fixture.getRound().getRoundNumber(),
                fixture.getScheduledTime());
    }
}
