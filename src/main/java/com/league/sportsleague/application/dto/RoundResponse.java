package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Round;

import java.time.LocalDate;
import java.util.UUID;

public record RoundResponse(
        UUID id,
        int roundNumber,
        String name,
        String status,
        LocalDate startDate,
        int fixtureCount
) {
    public static RoundResponse from(Round round) {
        return new RoundResponse(
                round.getId(),
                round.getRoundNumber(),
                round.getName(),
                round.getStatus().name(),
                round.getStartDate(),
                round.getFixtures().size()
        );
    }
}
