package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Fixture;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record FixtureResponse(
        UUID id,
        UUID roundId,
        int roundNumber,
        String roundName,
        String homeTeamCode,
        String homeTeamName,
        UUID homeTeamId,
        String awayTeamCode,
        String awayTeamName,
        UUID awayTeamId,
        Integer homeScore,
        Integer awayScore,
        String status,
        Instant scheduledTime,
        List<MatchEventResponse> events,
        UUID seasonId,
        UUID leagueId
) {
    public static FixtureResponse from(Fixture fixture) {
        List<MatchEventResponse> events = fixture.getEvents().stream()
                .sorted(Comparator.comparingInt(e -> e.getMatchMinute().value()))
                .map(MatchEventResponse::from)
                .toList();
        return new FixtureResponse(
                fixture.getId(),
                fixture.getRound().getId(),
                fixture.getRound().getRoundNumber(),
                fixture.getRound().getName(),
                fixture.getHomeTeam().getShortCode().value(),
                fixture.getHomeTeam().getName(),
                fixture.getHomeTeam().getId(),
                fixture.getAwayTeam().getShortCode().value(),
                fixture.getAwayTeam().getName(),
                fixture.getAwayTeam().getId(),
                fixture.getHomeScore(),
                fixture.getAwayScore(),
                fixture.getStatus().name(),
                fixture.getScheduledTime(),
                events,
                fixture.getRound().getSeason().getId(),
                fixture.getRound().getSeason().getLeague().getId()
        );
    }
}
