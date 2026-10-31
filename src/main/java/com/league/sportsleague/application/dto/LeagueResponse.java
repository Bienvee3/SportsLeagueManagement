package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.League;

import java.time.Instant;
import java.util.UUID;

public record LeagueResponse(
        UUID id,
        String name,
        String slug,
        String sportType,
        String country,
        Instant createdAt
) {
    public static LeagueResponse from(League league) {
        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getSlug().value(),
                league.getSportType(),
                league.getCountry(),
                league.getCreatedAt()
        );
    }
}
