package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Team;

import java.util.UUID;

public record TeamResponse(
        UUID id,
        String name,
        String shortCode,
        String stadiumName,
        String city,
        Integer foundedYear
) {
    public static TeamResponse from(Team team) {
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getShortCode().value(),
                team.getStadiumName(),
                team.getCity(),
                team.getFoundedYear()
        );
    }
}
