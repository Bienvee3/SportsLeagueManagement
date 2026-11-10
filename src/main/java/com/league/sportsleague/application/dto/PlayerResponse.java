package com.league.sportsleague.application.dto;

import com.league.sportsleague.domain.model.Player;

import java.util.UUID;

public record PlayerResponse(
        UUID id,
        UUID teamId,
        String name,
        int shirtNumber,
        String position
) {
    public static PlayerResponse from(Player player) {
        return new PlayerResponse(
                player.getId(),
                player.getTeam().getId(),
                player.getName(),
                player.getShirtNumber(),
                player.getPlayingPosition()
        );
    }
}
